#!/usr/bin/env python3
"""
lrepl — Lisp-like live-image REPL for Kotlin/Wasm.

Each REPL submission becomes one tiny wasm module that
re-declares every class seen so far, @WasmImport's the previous bindings it
references from the daemon's registry, and declares only the new code.
Redefining a function loads a new module whose cells make the new body
live for every caller (load IS apply). No Gradle anywhere.

The driver is deliberately thin: parsing and rewriting happen in the
compile server's `parse` mode (real Kotlin PSI — see CompileServer.kt), which
returns final texts and the exact set of bindings they use. This file owns
only REPL policy (cells, semantics, registry) and the compile/daemon plumbing.

Requires the local compiler patch relaxing @WasmImport/@WasmExport signature
types (see ../docs/ROADMAP.md §7).
"""

import base64
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time

HERE = os.path.dirname(os.path.abspath(__file__))
KOTLIN_DIST = os.environ.get("KOTLIN_DIST", os.path.join(HERE, "..", "..", "dist"))
KOTLINC_WASM = os.path.join(KOTLIN_DIST, "kotlinc", "bin", "kotlinc-wasm")
STDLIB = os.path.join(KOTLIN_DIST, "kotlinc", "lib", "kotlin-stdlib-wasm-wasi.klib")
WORK = os.environ.get("LREPL_WORK") or os.path.join(HERE, "build")
DAEMON = os.path.join(HERE, "daemon.mjs")
# the image's base wasm (linked from the stdlib klib itself — see
# Image._ensure_base) and the byproduct stdlib slice each delta link emits
# alongside the delta (ignored by the driver)
BASE_WASM = "base-full.wasm"

FILE_HEADER = "@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)"

ANSI_RE = re.compile(r'\x1b\[[0-9;]*m')


# ---------------------------------------------------------------- servers

class CompileServer:
    """Two resident JVMs from one server.jar:

    - the compile JVM is a thin shim forwarding each request to the
      official Kotlin compile daemon (the same resident service Gradle
      drives — it already speaks the wasm target, and it restarts itself
      when the dist's jars change);
    - the parse JVM runs the Kotlin PSI frontend (separate process: the
      IntelliJ Application is a JVM-wide singleton).

    Both fall back to nothing gracefully-ish: without the compile JVM we
    shell out to kotlinc-wasm; without the parse JVM the driver cannot run.
    """

    US = "\x01"

    def __init__(self):
        self.proc = None
        self.parse_proc = None
        self.log = None

    def _shim_classpath(self):
        lib = os.path.join(KOTLIN_DIST, "kotlinc", "lib")
        return ":".join([
            os.path.join(lib, "kotlin-compiler.jar"),
            os.path.join(lib, "kotlin-daemon-client.jar"),
        ])

    def _build_jar(self):
        jar = os.path.join(HERE, "server.jar")
        if os.path.exists(jar):
            return jar
        r = subprocess.run(
            [os.path.join(KOTLIN_DIST, "kotlinc", "bin", "kotlinc"),
             "-cp", self._shim_classpath(),
             os.path.join(HERE, "CompileServer.kt"), "-d", jar],
            capture_output=True, text=True,
        )
        return jar if r.returncode == 0 else None

    def ensure_running(self):
        if self.proc and self.proc.poll() is None:
            return True
        jar = self._build_jar()
        if not jar:
            return False
        os.makedirs(WORK, exist_ok=True)
        self.log = open(os.path.join(WORK, "server.log"), "w")
        try:
            self.proc = subprocess.Popen(
                ["java", "-cp", f"{jar}:{self._shim_classpath()}",
                 "CompileServerKt"],
                stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.log, text=True,
            )
        except OSError:
            self.proc = None
            return False
        return True

    def compile(self, args, timeout=180):
        """(ok, diagnostics) or None → caller falls back to the CLI.
        timeout: warm compiles take seconds, so the default is tight; the
        caller passes a long one for the rare legitimately-slow compile
        (a wiped IC cache re-lowers the whole stdlib)."""
        if not self.ensure_running():
            return None
        try:
            self.proc.stdin.write(self.US.join(args) + "\n")
            self.proc.stdin.flush()
            # WATCHDOG: the shim is an RMI client of the compile daemon,
            # and RMI has NO timeout — a daemon death mid-request leaves
            # the shim blocked forever (the classic wedge). Bound the read;
            # on timeout the except below kills the shim and the caller
            # falls back to spawning kotlinc, so the loop always makes
            # progress. Generous limit: a cold-cache first line re-lowers
            # the stdlib and takes minutes.
            result = {}

            def _read():
                try:
                    result["parts"] = self.proc.stdout.readline().split(" ", 2)
                except Exception as e:  # noqa: BLE001
                    result["err"] = e

            reader = threading.Thread(target=_read, daemon=True)
            reader.start()
            reader.join(timeout)
            if reader.is_alive():
                raise TimeoutError(f"compile server timed out ({timeout}s)")
            if "err" in result:
                raise result["err"]
            parts = result["parts"]
            if len(parts) != 3 or parts[0] != "RESULT":
                raise RuntimeError("bad server response")
            diag = ANSI_RE.sub("", base64.b64decode(parts[2]).decode())
            return int(parts[1]) == 0, diag
        except Exception:  # noqa: BLE001 — fall back to the CLI
            try:
                self.proc.kill()
            except Exception:  # noqa: BLE001
                pass
            self.proc = None
            return None

    def parse(self, source, binding_spec=""):
        """Parse+rewrite one submission. Returns the server's JSON dict
        (see CompileServer.kt) or None when the parse JVM is unusable."""
        jar = self._build_jar()
        if not jar:
            return None
        if not (self.parse_proc and self.parse_proc.poll() is None):
            os.makedirs(WORK, exist_ok=True)
            try:
                self.parse_log = open(os.path.join(WORK, "parse-server.log"), "w")
                self.parse_proc = subprocess.Popen(
                    ["java", "-cp", f"{jar}:{self._shim_classpath()}",
                     "CompileServerKt", "parse"],
                    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.parse_log, text=True,
                )
            except OSError:
                self.parse_proc = None
                return None
        try:
            req = base64.b64encode(source.encode()).decode() + self.US + base64.b64encode(binding_spec.encode()).decode()
            self.parse_proc.stdin.write(req + "\n")
            self.parse_proc.stdin.flush()
            parts = self.parse_proc.stdout.readline().split(" ", 2)
            if len(parts) != 3 or parts[0] != "RESULT":
                raise RuntimeError("bad parse response")
            if int(parts[1]) != 0:
                return None
            return json.loads(base64.b64decode(parts[2]).decode())
        except Exception:  # noqa: BLE001
            try:
                self.parse_proc.kill()
            except Exception:  # noqa: BLE001
                pass
            self.parse_proc = None
            return None


# ---------------------------------------------------------------- input UX

def balanced(text):
    """True if (), [] and {} are balanced outside string/char literals.
    (Used only to decide when a terminal submission is complete.)"""
    depth = {'(': 0, '[': 0, '{': 0}
    closer = {')': '(', ']': '[', '}': '{'}
    quote = None
    i = 0
    while i < len(text):
        c = text[i]
        if quote:
            if c == '\\':
                i += 2
                continue
            if c == quote:
                quote = None
        elif c in ('"', "'"):
            quote = c
        elif c in depth:
            depth[c] += 1
        elif c in closer:
            depth[closer[c]] -= 1
            if depth[closer[c]] < 0:
                return True  # over-closed: stop accumulating
        i += 1
    return all(v == 0 for v in depth.values())


# ---------------------------------------------------------------- image

class Image:
    def __init__(self):
        self.classes = {}         # class name -> frozen declaration text (order-stable)
        self.class_refs = {}      # class name -> binding names it references
        self.bindings = {}         # name -> dict(kind, gen, sig, kotlin_type, export)
        self.gen = 0
        self.last_wasm_bytes = None   # newest module's DELTA bytes (sent to the daemon for load)
        self.base_bytes = None     # the multimodule STDLIB BASE's bytes (byte-identical across gens)
        self.pkg_overrides = {}    # (package, name, sig) -> decl text — builtin redefinitions
        self.base_sent = None      # the base bytes the daemon last instantiated
        self.wats = {}             # gen -> wat text (for :dis and type recovery)
        self.gen_exports = {}      # gen -> export names (hot-reload entries/accessors)
        self.builtin_overrides = {}  # simple name -> (gen, fqName) — display record
        self.dis_probes = {}       # wasm fn name -> probe gen numbers (force-linked)
        self.daemon = None
        self.msg_id = 0
        self.server = CompileServer()
        self.ic_cache = os.environ.get("LREPL_IC", "1") == "1" and os.path.join(WORK, "ic-cache")
        # The IC cache is image-scoped: wasm fragments embed payload
        # structs derived from the INJECTED registry (the image's class
        # history), and the cache key covers only source fingerprints and
        # config — not the registry content — so fragments from another
        # image would serve stale class layouts. (The fdefn encoding
        # itself bakes nothing; this wipe is the class-layout story.)
        # Wipe at image creation and on reset. The base wasm goes WITH
        # the wipe: the compiler's base re-emission skip keys on the
        # OUTPUT FILE's existence, so a kept file would make fresh deltas
        # link against a stale base (the same trap the browser loop
        # handles by deleting the base after every dist rebuild).
        self._wipe_ic_state()
        self.extra_imports = set()  # import lines harvested from submitted code

    # ---- registry helpers

    def _wipe_ic_state(self):
        """Wipe the IMAGE-scoped IC cache (the payload version history —
        fresh class declarations must not bump versions against ghosts).
        The base wasm is DIST-keyed, not image-keyed: it survives resets
        (_ensure_base stamps it against the stdlib klib and re-links it
        only when the dist changes), because a fresh image simply re-
        instantiates the same base bytes with a FRESH table."""
        if self.ic_cache and os.path.isdir(self.ic_cache):
            shutil.rmtree(self.ic_cache, ignore_errors=True)

    def _ensure_base(self):
        """The image's BASE = the FULL stdlib, linked once from the stdlib
        klib ITSELF (hot + multimodule) — NOT the app's reference closure,
        which is what the delta link emits as its stdlib byproduct: a REPL
        can reference ANY stdlib function in ANY later line, and a delta
        importing something outside the base's slice fails to link
        ("function import requires a callable"). It carries the image's
        fdefinition table, so the daemon instantiates it ONCE per image.

        Re-emission is keyed on the STDLIB KLIB's stamp (mtime): a dist
        rebuild changes the klib, the stamp mismatches, the base re-links.
        The link always passes -Xwasm-IC-generate-unchanged-modules: the
        stdlib artifact's fragments are unmodified in the warm ic-base
        cache, and without it the link skips emitting the base entirely
        (the emission skip keys on the IC cache's module info)."""
        if self.base_bytes:
            return True, None
        path = os.path.join(WORK, BASE_WASM)
        stamp_path = path + ".stamp"
        try:
            klib_stamp = os.stat(STDLIB).st_mtime_ns
        except OSError:
            klib_stamp = None
        if os.path.exists(path):
            try:
                with open(stamp_path) as f:
                    if f.read().strip() == str(klib_stamp):
                        self._read_base(path)
                        return True, None
            except OSError:
                pass  # no stamp: re-link below
        os.makedirs(WORK, exist_ok=True)
        # its own IC cache: the config hash covers the -ir-output-name,
        # so sharing the app's cache would thrash both
        ic_base = os.path.join(WORK, "ic-base")
        args = ["-Xwasm-target=wasm-wasi", f"-Xinclude={STDLIB}",
                "-ir-output-dir", WORK, "-ir-output-name", "base-full",
                "-main", "noCall", "-Xwasm-hot-reload",
                "-Xwasm-generate-closed-world-multimodule",
                "-Xwasm-IC-generate-unchanged-modules",
                f"-Xcache-directory={ic_base}"]
        # cold (a fresh ic-base re-lowers the whole stdlib) is minutes
        res = self.server.compile(args, timeout=600)
        if res is not None:
            ok, err = res
            err = "\n".join(l for l in err.splitlines() if l.strip())
        else:
            r = subprocess.run([KOTLINC_WASM, *args], capture_output=True, text=True)
            ok = r.returncode == 0
            err = "\n".join(l for l in (r.stderr + r.stdout).splitlines() if l.strip())
        if not ok:
            return False, err
        # the link names the stdlib artifact's wasm after itself
        shutil.move(os.path.join(WORK, "kotlin-kotlin-stdlib.wasm"), path)
        with open(stamp_path, "w") as f:
            f.write(str(klib_stamp))
        self._read_base(path)
        return True, None

    def _read_base(self, path):
        with open(path, "rb") as f:
            self.base_bytes = f.read()

    def all_class_refs(self):
        out = set()
        for s in self.class_refs.values():
            out |= s
        return out

    def binding_spec(self):
        """The binding table for the parse server: 'name=kind|name2=kind'."""
        return "|".join(f"{n}={b['kind']}" for n, b in self.bindings.items())

    # ---- daemon plumbing

    def daemon_send(self, msg):
        self.msg_id += 1
        msg["id"] = self.msg_id
        self.daemon.stdin.write((json.dumps(msg) + "\n").encode())
        self.daemon.stdin.flush()
        while True:
            line = self.daemon.stdout.readline()
            if not line:
                raise RuntimeError("daemon died")
            resp = json.loads(line)
            if resp.get("id") == self.msg_id:
                return resp

    def start_daemon(self):
        self.daemon = subprocess.Popen(
            ["node", "--no-warnings", DAEMON],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )

    def reset(self):
        self.classes = {}
        self.class_refs = {}
        self.bindings = {}
        self.gen = 0
        self.last_wasm_bytes = None
        self.base_bytes = None
        self.base_sent = None
        self.wats = {}
        self.gen_exports = {}
        self.builtin_overrides = {}
        self.pkg_overrides = {}
        self.dis_probes = {}
        # the IC cache holds the previous compilations' fragments — the
        # payload version history. A fresh image must not chain against it
        # (fresh class declarations would bump versions against ghosts).
        self._wipe_ic_state()
        if self.daemon:
            self.daemon_send({"cmd": "reset"})

    # ---- compile pipeline

    def compile_module(self, body_lines, imports_needed, exclude_classes=frozenset()):
        """Assemble, klib-compile, link and patch one module."""
        ok, err = self._ensure_base()
        if not ok:
            return False, err, getattr(self, "gen", 0)
        self.gen += 1
        gen = self.gen
        # (no stranded-binding guard in the fdefn era: there are no
        # install-time engine verdicts to echo — a reference to an old
        # instance either migrates lazily at first field access, works,
        # or traps honestly at the per-call cast)
        os.makedirs(WORK, exist_ok=True)
        # ONE evolving source path and ONE stable klib/module identity, so the
        # wasm IC cache accumulates the file's history — the previous
        # compilation's fragments (old payload structs) become a compiler
        # input, which is how the backend chains payload versions for
        # redefined classes (DESIGN §4b; no CLI args involved). Per-load
        # names would give the cache a fresh library every line — history
        # would never accumulate.
        src_path = os.path.join(WORK, "image.kt")
        klib = os.path.join(WORK, "image.klib")
        wasm_path = os.path.join(WORK, f"gen{gen}.wasm")

        imports = self.gen_import_lines(imports_needed)
        header = [FILE_HEADER]
        header += sorted(self.extra_imports)
        frozen = [t for n, t in self.classes.items() if n not in exclude_classes]
        source = "\n".join([*header, "", *frozen, "", *imports, "", *body_lines, ""])
        with open(src_path, "w") as f:
            f.write(source)

        # BUILTIN REDEFINITIONS ride their own package files (a binding
        # redeclared under its stdlib package has the SAME binding key —
        # the module's resolve then installs into the EXISTING cell, the
        # native CL defun semantics; the flag relaxes the frontend's
        # kotlin-package guard for these compiles only)
        pkg_files = []
        kotlin_pkg = []
        if self.pkg_overrides:
            by_pkg = {}
            for (_pkg, _n, _t), text in sorted(self.pkg_overrides.items()):
                by_pkg.setdefault(_pkg, []).append(text)
            for pkg, texts in sorted(by_pkg.items()):
                fpath = os.path.join(WORK, "ovr_" + pkg.replace(".", "_") + ".kt")
                with open(fpath, "w") as f:
                    # the override bodies may reference user bindings: the
                    # host-funcref externals are package-independent
                    f.write("\n".join([FILE_HEADER, "", f"package {pkg}", "",
                                        *imports, "", *texts, ""]))
                pkg_files.append(fpath)
            kotlin_pkg = ["-Xallow-kotlin-package"]

        ic = [f"-Xcache-directory={self.ic_cache}"] if self.ic_cache else []
        hot = ["-Xwasm-hot-reload", "-Xwasm-generate-closed-world-multimodule"]
        # fdefn mode (BOTH targets now): every reloadable function dispatches
        # through its fdefinition cell (load is apply), and the link emits
        # MULTIMODULE output — the resident STDLIB BASE (byte-identical
        # across submissions, instantiated once per image: the base IS the
        # image, its stdlib owns the fdefinition table) plus this
        # module's app DELTA.
        # gen 1 (or a fresh cache) re-lowers the stdlib once — minutes;
        # every other line is warm and must answer in 180s
        compile_timeout = 600 if (self.gen == 1 or not self.ic_cache) else 180
        for args in (
            ["-Xwasm-target=wasm-wasi", src_path, *pkg_files, "-ir-output-dir", WORK,
             "-ir-output-name", "image", "-libraries", STDLIB, *hot, *kotlin_pkg, *ic],
            # NB the link step's -ir-output-name must be STABLE across
            # modules: it participates in the IC cache's config hash, so
            # a per-module name gives every module a fresh cache and
            # the payload version history never accumulates. The delta wasm
            # (named after the app klib's module identity, not the link
            # name) is renamed to gen{N}.wasm after the link.
            ["-Xwasm-target=wasm-wasi", f"-Xinclude={klib}", "-libraries", STDLIB,
             "-ir-output-dir", WORK, "-ir-output-name", "image-linked", "-main", "noCall",
             *hot, *kotlin_pkg, *ic],
        ):
            res = self.server.compile(args, timeout=compile_timeout)
            if res is not None:
                ok, err = res
                err = "\n".join(l for l in err.splitlines() if l.strip())
            else:
                r = subprocess.run([KOTLINC_WASM, *args], capture_output=True, text=True)
                ok = r.returncode == 0
                err = "\n".join(l for l in (r.stderr + r.stdout).splitlines() if l.strip())
            if not ok:
                return False, err, gen
        # The image side runs on IN-MEMORY bytes (like CL's in-image
        # compile+load: nothing about the running image reads the fs). Hot
        # modules are REACTOR-STYLE natively (no start section; init
        # exported as _initialize by the compiler) — no binary patching.
        # The multimodule link emits the app DELTA (_image_.wasm, named
        # after the app klib's module identity) plus a pruned stdlib
        # SLICE byproduct this driver ignores — the image's base is the
        # FULL stdlib from _ensure_base, and the registry injection reads
        # the DELTA's custom section. The gen file is a best-effort DEBUG
        # dump (for :dis) — the runtime never reads it back.
        with open(os.path.join(WORK, "_image_.wasm"), "rb") as f:
            self.last_wasm_bytes = f.read()
        try:
            with open(wasm_path, "wb") as f:
                f.write(self.last_wasm_bytes)
        except OSError:
            pass  # debug dump only; the runtime does not need it
        return True, None, gen

    def load_module(self, gen, imports_needed):
        host = {}
        for name in imports_needed:
            b = self.bindings[name]
            if b["kind"] == "fun":
                for key, m in b["methods"].items():
                    host[f"{name}${key}"] = [m["gen"], m["export"]]
            else:
                host[f"{name}__get"] = [b["gen"], b["export"]]
                if b.get("has_set"):
                    host[f"{name}__set"] = [b["gen"], f"{name}__set"]
        # the wasm travels as bytes — the daemon never reads the fs; the
        # LOAD IS APPLY: the module's init resolves its bindings' cells by
        # name against the image and installs its impls — nothing to size,
        # nothing to sweep. The BASE rides the FIRST load of an image (and
        # any time its bytes changed — a dist rebuild) — the daemon
        # instantiates it once; its stdlib IS the image's fdefinition table
        load = {"cmd": "load", "gen": gen,
                "wasmB64": base64.b64encode(self.last_wasm_bytes).decode(),
                "imports": host}
        if self.base_bytes and (gen == 1 or self.base_bytes != getattr(self, "base_sent", None)):
            load["baseB64"] = base64.b64encode(self.base_bytes).decode()
            self.base_sent = self.base_bytes
        resp = self.daemon_send(load)
        if not resp.get("ok"):
            return False, resp.get("error", "instantiation failed")
        self.last_load_exports = resp.get("exports") or []
        self.gen_exports[gen] = self.last_load_exports
        # NB builtin overrides persist in the image's fdefinition table:
        # a later module re-declaring the same binding key resolves
        # the SAME cell, so the override holds (DESIGN §15)
        return True, None

    def call(self, gen, fn):
        return self.daemon_send({"cmd": "call", "gen": gen, "fn": fn})

    def find_export(self, prefix):
        """The compiler's hot-reload export names carry a reference-key hash
        suffix (overload disambiguation) — find them by prefix at load time."""
        matches = [n for n in self.last_load_exports if n.startswith(prefix)]
        if len(matches) != 1:
            return None
        return matches[0]

    def wat(self, gen):
        if gen not in self.wats:
            r = subprocess.run(["wasm-tools", "print", os.path.join(WORK, f"gen{gen}.wasm")],
                               capture_output=True, text=True)
            self.wats[gen] = r.stdout if r.returncode == 0 else ""
        return self.wats[gen]

    # ---- source assembly helpers

    def gen_import_lines(self, needed):
        lines = []
        for name in sorted(needed):
            b = self.bindings[name]
            if b["kind"] == "fun":
                if b.get("pkg"):
                    # a BUILTIN REDEFINITION: no host-funcref import — new
                    # callers reach it through the harvested stdlib import,
                    # which binds to the same cell the override installed
                    # into (and the override file must not import itself)
                    continue
                for key, m in sorted(b["methods"].items()):
                    iname = f"{name}${key}".replace("$", "\\$")  # escape for Kotlin literals
                    lines += [f'@WasmImport("host", "{iname}")', f"external {m['sig']}"]
            else:
                t = b["kotlin_type"]
                lines += [f'@WasmImport("host", "{name}__get")', f"external fun {name}__get(): {t}"]
                if b.get("has_set"):
                    lines += [f'@WasmImport("host", "{name}__set")', f"external fun {name}__set(v: {t})"]
        return lines

    # ---- type recovery

    def recover_val_type(self, gen, accessor):
        """Map the accessor's wasm result type back to a Kotlin source type."""
        lines = self.wat(gen).splitlines()
        hdr = re.compile(
            rf'^  \(func \$"?{re.escape(accessor)}"? \(;\d+;\) \(type (\$"[^"]*"|\$[^\s(;]+)'
        )
        token = None
        for l in lines:
            m = hdr.match(l)
            if m:
                token = m.group(1)
                break
        if not token:
            return None
        decl = find_type_decl(lines, token)
        if decl is None:
            return None
        rm = re.search(r'\(result (.*)\)\)\)$', decl)
        return self.wat_result_to_kotlin(rm.group(1)) if rm else None

    def wat_result_to_kotlin(self, res):
        res = res.rstrip(')').strip()
        prim = {"i32": "Int", "i64": "Long", "f32": "Float", "f64": "Double"}
        if res in prim:
            return prim[res]  # NB: i32 is ambiguous (Int/Boolean/UInt/Char) — Int wins
        nm = re.match(r'\(ref(?:\s+null)?\s+\$"?([^")\s]+)', res)
        if nm:
            n = nm.group(1)
            base = None
            if n in self.classes:
                base = n
            elif n == "kotlin.String":
                base = "String"
            elif n == "kotlin.Any":
                base = "Any"
            if base:
                # wasm globals are represented nullable, but the Kotlin
                # declaration type is what we re-import; use the non-null form
                return base
        return None


# ---------------------------------------------------------------- disassembly

def find_type_decl(wat_lines, token):
    for l in wat_lines:
        if l.startswith(f"  (type {token} ") or l.startswith(f"  (type {token}("):
            return l
    return None


def func_block_slices(wat_lines, namepat):
    """Top-level (func ...) blocks whose wasm name matches namepat(token),
    each preceded by its (type ...) declaration line."""
    tok_re = re.compile(r'^  \(func \$([^ (;]+) \(;')
    blocks = []
    idx = 0
    while idx < len(wat_lines):
        m = tok_re.match(wat_lines[idx])
        if m and namepat(m.group(1)):
            block = [wat_lines[idx]]
            for l2 in wat_lines[idx + 1:]:
                if re.match(r'^  \(', l2) or l2 == ')':
                    break
                block.append(l2)
            decl = None
            tm = re.search(r' \(type (\$"[^"]*"|\$[^\s(;]+)', wat_lines[idx])
            if tm:
                decl = find_type_decl(wat_lines, tm.group(1))
            blocks.append("\n".join(([decl] if decl else []) + block))
            idx += len(block)
        else:
            idx += 1
    return blocks


def extract_dis_all(image, name, gen):
    """Disassemble every function with the given wasm name (overloads share
    names — e.g. all kotlin.io.println variants are named kotlin.io.println)."""
    return func_block_slices(image.wat(gen).splitlines(),
                             lambda tok: tok.strip('"') == name)


def extract_dis(image, name, gen):
    """Slice a function (and its type) out of a module's wat text."""
    blocks = extract_dis_all(image, name, gen)
    return (blocks[0], gen) if blocks else (None, None)


# ---------------------------------------------------------------- REPL core

class Lrepl:
    def __init__(self):
        self.image = Image()
        self.image.start_daemon()
        self.history = []

    # ---- generic compile-load-register cycle

    def try_line(self, body, register, explicit_refs=frozenset(), exclude_classes=frozenset()):
        image = self.image
        refs = set(explicit_refs) | image.all_class_refs()
        ok, err, gen = image.compile_module(body, refs, exclude_classes=exclude_classes)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"
        out = register(gen)
        return True, out

    # ---- entry point

    def handle(self, line):
        image = self.image

        # REPL commands work from every frontend (terminal, swank, comint)
        if line == ":reset":
            image.reset()
            return True, "image cleared"
        if line == ":list":
            out = [f"classes: {len(image.classes)}"]
            for n, b in image.bindings.items():
                if b.get("kind") == "fun":
                    methods = " | ".join(f"{k} [gen {m['gen']}]" for k, m in sorted(b["methods"].items()))
                    out.append(f"  fun  {n}: {methods}")
                else:
                    out.append(f"  {b['kind']:4} {n}: {b.get('kotlin_type')}  [gen {b['gen']}]")
            if image.builtin_overrides:
                out.append("  redefined stdlib: " +
                           ", ".join(sorted(fq for _g, fq in image.builtin_overrides.values())))
            return True, "\n".join(out)
        if line.startswith(":dis"):
            arg = line[4:].strip()
            return self.run_dis(arg)


        p = image.server.parse(line, image.binding_spec())
        if p is None:
            return False, "parser unavailable — the compile-server jar is missing or the parse JVM died (see build/parse-server.log)"
        return self.handle_parsed(p)

    def run_dis(self, arg):
        """Disassemble a binding (following its cell to the actual body),
        or any function by wasm name (stdlib/overloads) in the newest gen."""
        img = self.image
        if not arg:
            return False, "usage: :dis NAME  (a binding, a class, or a wasm function name like kotlin.io.println)"
        if arg in img.classes or f"{arg}__obj" in img.classes:
            return self.dis_class(arg)
        if arg in img.bindings:
            b = img.bindings[arg]
            if b["kind"] == "fun":
                return self.dis_fun_binding(arg, b)
            wat_txt, gen = extract_dis(img, f"{arg}__get", b["gen"])
            return (True, wat_txt) if wat_txt else (False, f"no function '{arg}' found")

        # not a binding: any function by wasm name (stdlib, overloads).
        # DCE links only what each module uses, so search newest to oldest.
        for gen in range(img.gen, 0, -1):
            if gen not in img.gen_exports:
                continue
            blocks = extract_dis_all(img, arg, gen)
            if blocks:
                head = f"gen {gen}: {len(blocks)} function(s) named {arg}"
                return True, head + "\n\n" + "\n\n".join(blocks)

        # not linked anywhere yet: compile a probe module that force-links
        # it (an exported stub call — exports are DCE roots, and the slot chain
        # drags the callee in), then disassemble from the probe.
        if arg in img.dis_probes:
            status, probe_gens = img.dis_probes[arg]
        else:
            status, probe_gens = self.probe_overloads(arg)
            if status != "unresolved":
                img.dis_probes[arg] = (status, probe_gens)
        if status == "found":
            blocks = []
            for gen in probe_gens:
                blocks += extract_dis_all(img, arg, gen)
            head = f"force-linked probe: {len(blocks)} function(s) named {arg}"
            return True, head + "\n\n" + "\n\n".join(blocks)
        if status == "inline":
            return False, (arg + " has no standalone wasm function — likely an inline "
                          "function; its body is emitted at each call site")
        if status == "shape":
            return False, ("the name " + arg + " resolves, but no probe call shape matched "
                          "(extension receiver or lambda parameters?). Reference the "
                          "function in code to link it, then retry")
        return False, ("no function named '" + arg + "' resolves — check the fully "
                       "qualified name (e.g. :dis kotlin.math.sin)")

    def dis_class(self, name):
        """The whole class from the newest module's wat: the cell struct,
        its payload LAYOUT struct (the class's current shape — the
        payload type key is the class key), the vtable type, the slot/RTTI globals,
        and every function belonging to the class — the constructor,
        methods and accessors, and the layout machinery (the per-slot
        reader/writer closures and the allocate the image's migration
        dispatches through)."""
        img = self.image
        klass = name if name in img.classes else f"{name}__obj"
        lines = img.wat(img.gen).splitlines()
        out = [f"class {klass} — module {img.gen}"]

        def type_lines(pat):
            cre = re.compile(pat)
            return [l for l in lines if cre.search(l)]

        esc = re.escape(klass)
        # --- types: the cell, its payload versions, the vtable (nameless
        # #typeNNN <classVTable> — found via the cell's own vtable field)
        cell = type_lines(rf'^\s*\(type \${esc} \(;')
        if not cell:
            return False, f"class {klass} not linked in module {img.gen}"
        out.append("\n  ;; cell struct (stable forever: vtable, itable, rtti, payload)")
        out.extend(cell)
        payloads = type_lines(rf'^\s*\(type \$"?#?(?:type\d+ )?{esc}\$payload')
        if payloads:
            out.append("\n  ;; payload layout struct (the class's CURRENT shape; the payload type key is the class key)")
            out.extend(payloads)
        vt = re.search(r'\(ref (\$"#type\d+ <classVTable>")\)', cell[0])
        if vt:
            vtl = type_lines(rf'^\s*\(type {re.escape(vt.group(1))} \(;')
            if vtl:
                out.append("\n  ;; vtable type")
                out.extend(vtl)
        # --- globals: the class's RTTI
        rtti_globals = [l for l in lines if f"{klass}_rtti" in l or f"{klass}._rtti" in l]
        if rtti_globals:
            out.append("\n  ;; globals: RTTI")
            out.extend(rtti_globals)
        # --- member functions: constructor, methods, accessors
        members = func_block_slices(lines, lambda tok: tok.startswith(klass + "."))
        if members:
            out.append(f"\n  ;; member functions ({len(members)})")
            out.extend(members)
        # --- hot-reload layout machinery (the slot accessors the image's
        # generic migration dispatches through)
        machinery = func_block_slices(
            lines, lambda tok: bool(re.match(rf'__hot_slot(?:Read|Write|Allocate)\$.*{esc}(?:[.#]|$)', tok))
        )
        if machinery:
            out.append(f"\n  ;; hot-reload machinery: layout readers/writers, allocate ({len(machinery)})")
            out.extend(machinery)
        return True, "\n".join(out)

    # common stdlib signatures: (return type, argument literal). A compile
    # failure just means this overload shape doesn't exist.
    PROBE_CANDIDATES = [
        ("Int", "1"),
        ("Double", "1.0"),
        ("Long", "1L"),
        ("Float", "1.0f"),
        ("String", "\"\""),
        ("Unit", "\"\""),
        ("Boolean", "true"),
        ("Char", "'a'"),
        ("Unit", "1"),
        ("Unit", "null"),
        ("Int", "1, 2"),
        ("Boolean", "\"\", \"\""),
        ("Int", "\"\""),
        ("Boolean", "null"),
    ]
    PROBE_MAX_OVERLOADS = 4

    def dis_fun_binding(self, name, b):
        """Show every method's body: the impl the binding's cell holds
        lives in the module that defined it, under its plain wasm
        name (the entry export name carries the binding key; the impl
        carries the name — entries read the cell, the impl IS the body)."""
        img = self.image
        outs = []
        for key, m in b.get("methods", {}).items():
            wat_name = m["export"][len("__hot_entry$"):].rsplit("#", 1)[0]
            body, gen = extract_dis(img, wat_name, m["gen"])
            if body:
                outs.append(f";; method {key}  [gen {gen} impl]\n{body}")
        if outs:
            return True, "\n\n".join(outs)
        return False, f"no disassemblable body found for '{name}'"

    def probe_overloads(self, fq_or_simple):
        """Force-link a stdlib function by compiling exported stub calls in a
        range of common signatures. Returns (status, gens):
          "found"       — gens contain disassemblable overloads
          "inline"      — a probe compiled but the function has no standalone
                          wasm body (inline function)
          "shape"       — the name resolves but no probe call shape matched
                          (extension receiver / lambda parameters)
          "unresolved"  — no such function in scope
        """
        img = self.image
        simple = fq_or_simple.split(".")[-1]
        import_line = "import " + fq_or_simple if "." in fq_or_simple else None
        if import_line:
            img.extra_imports.add(import_line)
        # NB probe modules are LOADED because they are CALLED — an
        # uninstantiated module's exports cannot answer; the probe's
        # fragments are one more (harmless) entry of image history.
        found_gens = []
        any_compiled = False
        # "unresolved '<simple>'" with a resolving import = extension mis-call
        # (no receiver); unresolved on a PACKAGE segment = no such function.
        pkg_unresolved = False
        simple_unresolved = False
        try:
            for ret, args in self.PROBE_CANDIDATES:
                body = ["@WasmExport", f"fun __probe(): {ret} = {simple}({args})"]
                ok, err, gen = img.compile_module(body, set())
                if not ok:
                    names = re.findall(r"unresolved reference '([^']+)'", err)
                    if any(n != simple for n in names):
                        # a package/qualifier segment failed to resolve
                        pkg_unresolved = True
                    elif names:
                        # only the simple name failed: with a resolving import
                        # that's an extension mis-call (no receiver)
                        simple_unresolved = True
                    continue
                any_compiled = True
                ok2, err2 = img.load_module(gen, set())
                if not ok2:
                    continue
                # the wasm function's wat name is the FULL name (kotlin.math.sin),
                # not the simple name the stub calls
                if extract_dis_all(img, fq_or_simple, gen):
                    found_gens.append(gen)
                    if len(found_gens) >= self.PROBE_MAX_OVERLOADS:
                        break
        finally:
            if import_line:
                img.extra_imports.discard(import_line)
        if found_gens:
            return "found", found_gens
        if any_compiled:
            return "inline", []
        if pkg_unresolved:
            return "unresolved", []
        return "shape", []

    def handle_parsed(self, p):
        image = self.image
        if p.get("imports"):
            image.extra_imports.update(f"import {i}" for i in p["imports"])
            if not p.get("decls"):
                return True, "import noted — applies to all subsequent submissions"

        if p.get("ok") and p.get("decls"):
            outs = []
            for d in p["decls"]:
                kind = d["kind"]
                if kind == "fun":
                    ok, o = self.handle_fun_ast(d)
                elif kind in ("class", "typealias"):
                    ok, o = self.handle_class_ast(d)
                elif kind == "object":
                    ok, o = self.handle_object_ast(d)
                elif kind == "property":
                    ok, o = self.handle_val_ast(d)
                else:
                    ok, o = False, f"unsupported top-level declaration: {kind}"
                if not ok:
                    return False, o
                outs.append(o)
            return True, "\n".join(outs)

        expr = p.get("asExpression") or {}
        block = p.get("asBlock") or {}
        # an "expression" containing assignments is really a statement
        if expr.get("ok") and not expr.get("assignCount", 0):
            ok, out = self.handle_expr_ast(expr)
            if ok:
                return ok, out
            if block.get("ok"):
                return self.handle_stmt_ast(block)
            return False, out
        if block.get("ok"):
            return self.handle_stmt_ast(block)
        if p.get("ok") and not p.get("decls"):
            return True, "ok"  # comments / package or file-annotation lines only
        if p.get("errorMessage"):
            return False, f"syntax error: {p['errorMessage']}"
        return False, "cannot parse submission"

    # ---- declaration handlers (consume the parse server's final texts)

    def handle_fun_ast(self, d):
        name = d["name"]
        if not name or name.startswith("__"):
            return False, "invalid function name"
        params = [(p["name"], p["type"]) for p in d["params"]]
        parsed = {
            "name": name,
            "param_list": d["paramList"],
            "ret": d.get("ret"),
            "text": d["text"],  # whole declaration, rewritten by the parse server
            "fn_type": f"({', '.join(t for _, t in params)}) -> {d.get('ret') or 'Unit'}",
        }
        return self.handle_fun_slot(parsed, explicit_refs=set(d.get("used") or []))

    def handle_class_ast(self, d):
        """Class definition/redefinition with ZERO parse-level class knowledge.

        The class's shape is fingerprinted by the compiler into every method's
        wasm type (the receiver parameter) and into the rec group; the ENGINE
        recognizes how the class changed (rec-group canonicalization) and
        judges every swap. The driver only does nominal matching (class name,
        method name segments in export names) and orchestration:

        - body-only redefinition (same fields, same method signatures): the
          new module's method entries are the same bindings — the load
          applies its bodies, and EXISTING INSTANCES run them via their
          vtables (vtable entries read the fdefn cells);
        - any shape change (fields, method signatures/set, supertypes): the
          receiver type differs — every swap is rejected; old instances
          remain the old type (coexistence), new instances are the new
          type. Reported honestly either way."""
        image = self.image
        name = d["name"]
        new_text = d["text"]
        used = set(d.get("used") or [])
        redefinition = name in image.classes

        def register(gen):
            if not redefinition:
                image.classes[name] = new_text
                image.class_refs[name] = used
                return f"declared {d['kind']} {name}"
            image.classes[name] = new_text
            image.class_refs[name] = used
            # LOAD IS APPLY: the module's init already installed its
            # method bodies into the cells (the newest definitions are
            # live, unconditionally — the fdefinition is type-erased)
            report = (f"redefined class {name} — the load applied its method bodies; "
                      "existing instances migrate on first new-code field access, "
                      "and calls whose wasm types no longer match trap at the "
                      "fdefn cast (SBCL semantics)")
            return report

        body = [new_text]
        exclude = frozenset({name}) if redefinition else frozenset()
        return self.try_line(body, register, explicit_refs=used,
                             exclude_classes=exclude)

    def handle_object_ast(self, d):
        """Object redefinition = class redefinition of <name>__obj with the
        binding's accessor kept stable: the singleton the registry points at
        is the OLD module's instance, so the method-slot sweep is what
        makes its bodies update (same engine-judged rules as classes)."""
        image = self.image
        name = d["name"]
        if not name or not d.get("classText"):
            return False, "cannot transform this object declaration"
        used = set(d.get("used") or [])
        klass = f"{name}__obj"
        redefinition = klass in image.classes

        def register(gen):
            image.classes[klass] = d["classText"]
            image.class_refs[klass] = used
            existing = image.bindings.get(name)
            if existing is not None and existing.get("kind") == "obj":
                # keep the binding pointed at the ORIGINAL singleton (state
                # survives); the sweep updates the bodies it dispatches to
                if redefinition:
                    return (f"redefined object {name} — the load applied its method "
                            "bodies; the live singleton dispatches to them through "
                            "its cell-backed vtable")
                return f"defined object {name} (state is shared across the image)"
            image.bindings[name] = {"kind": "obj", "gen": gen, "kotlin_type": klass, "export": f"{name}__get"}
            return f"declared object {name} (state is shared across the image)"

        body = [
            d["classText"],
            f"val {name} = {name}__obj()",
            f'@WasmExport("{name}__get")',
            f"fun {name}__get() = {name}",
        ]
        exclude = frozenset({klass}) if redefinition else frozenset()
        return self.try_line(body, register, explicit_refs=used,
                             exclude_classes=exclude)

    def handle_val_ast(self, d):
        name = d["name"]
        if not name or name.startswith("__"):
            return False, "invalid property name"
        if d.get("initializer") is None:
            return False, f"property '{name}' has no initializer"
        kind = "var" if d["isVar"] else "val"
        return self.handle_val_core(kind, name, d.get("declaredType"), d["initializer"],
                                    set(d.get("used") or []))

    # ---- function policy

    def handle_fun_slot(self, parsed, explicit_refs=frozenset()):
        """Julia-semantics definitions on compiler slots (-Xwasm-hot-reload).

        A name is a generic FUNCTION; each (name, signature) is a METHOD with
        its own slot. Same-signature definition REPLACES that method (live
        swap — all callers see the new body); a new signature ADDS a method —
        Kotlin overloading, so nothing is invalidated and old callers keep
        working. The engine's setter type check is the judge of "same
        signature" where the textual proxy is ambiguous."""
        image = self.image
        name = parsed["name"]
        if name.startswith("__"):
            return False, "names starting with __ are reserved by the REPL"
        fn_type = parsed["fn_type"]
        param_list = parsed["param_list"]
        ret = parsed["ret"]
        sig = f"fun {name}({param_list})" + (f" : {ret}" if ret else "")
        decl_text = parsed["text"]  # the plain declaration; the compiler slots it
        refs = (set(explicit_refs) - {name}) | image.all_class_refs()

        existing = image.bindings.get(name)
        if existing is not None and existing.get("kind") != "fun":
            existing = None  # kind change (val -> fun): the function is new

        # ---- BUILTIN SHADOW: the bare name resolved (in already-compiled
        # lines) to a dotted stdlib import — emit the definition UNDER THE
        # BUILTIN'S PACKAGE, so its binding key equals the stdlib's and
        # the module's resolve installs into the EXISTING cell. CL's
        # unshadowed defun, natively: one binding, every caller — old and
        # new — runs the new body. No move, no discovery, no evals.
        shadow_pkg = (existing or {}).get("pkg") or (
            self._builtin_shadow_pkg(name) if existing is None else None
        )
        if shadow_pkg:
            image.pkg_overrides[(shadow_pkg, name, fn_type)] = decl_text
            ok, err, gen = image.compile_module([], refs)
            if not ok:
                return False, err
            ok, err = image.load_module(gen, refs)
            if not ok:
                return False, f"instantiation failed: {err}"
            new_entry = image.find_export(f"__hot_entry${shadow_pkg}.{name}#")
            if not new_entry:
                return False, f"hot-reload entry not found for {shadow_pkg}.{name}"
            method = {"gen": gen, "export": new_entry, "fn_type": fn_type, "sig": sig}
            if existing is None:
                image.bindings[name] = existing = {"kind": "fun", "pkg": shadow_pkg,
                                                  "methods": {}}
                image.builtin_overrides[name] = (gen, f"{shadow_pkg}.{name}")
            existing["methods"][fn_type] = method
            return True, (f"redefined builtin {shadow_pkg}.{name}{fn_type} — the load "
                         "applied it into the stdlib binding's cell; already-compiled "
                         "callers now run the new body")

        ok, err, gen = image.compile_module([decl_text], refs)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"
        new_entry = image.find_export(f"__hot_entry${name}#")
        if not new_entry:
            return False, f"hot-reload entry not found for {name}"

        method = {"gen": gen, "export": new_entry, "fn_type": fn_type, "sig": sig}

        if existing is not None and fn_type in existing["methods"]:
            # ---- REPLACE the method: the module's init already
            # installed the new body into the binding's cell — every
            # caller reads the cell and now runs it (LOAD IS APPLY).
            existing["methods"][fn_type] = method
            return True, (f"redefined fun {name}{fn_type} — the load applied it; "
                         "existing callers now run the new body")

        if existing is None:
            # ---- first definition of the name
            existing = {"kind": "fun", "methods": {}}
            image.bindings[name] = existing
            existing["methods"][fn_type] = method
            return True, f"defined fun {name}{fn_type}"

        # ---- ADD a method (Julia: new method of the generic function).
        existing["methods"][fn_type] = method
        others = ", ".join(k for k in existing["methods"] if k != fn_type)
        return True, (f"defined fun {name}{fn_type} — new overload of {name} "
                      + (f"(existing: {others})" if others else "")
                      + "; existing methods and their callers are untouched")

    def _builtin_shadow_pkg(self, name):
        """The stdlib package a bare `name` resolved to in already-compiled
        lines — extra_imports holds the harvested imports, so a definition
        with this simple name REDEFINES the builtin."""
        for line in self.image.extra_imports:
            m = re.match(r"import\s+([a-zA-Z_][\w.]*)\.([a-zA-Z_]\w*)$", line)
            if m and m.group(2) == name:
                return m.group(1)
        return None

    # ---- property policy

    def handle_val_core(self, kind, name, annot, rhs, used):
        """Shared property machinery. `rhs` must already be rewritten."""
        image = self.image
        is_var = kind == "var"

        def body_with(get_only_type=None):
            decl = f"{kind} {name}" + (f": {annot}" if annot else "") + f" = {rhs}"
            out = [decl, f'@WasmExport("{name}__get")', f"fun {name}__get() = {name}"]
            if is_var and (annot or get_only_type):
                t = annot or get_only_type
                out += [f'@WasmExport("{name}__set")', f"fun {name}__set(v: {t}) {{ {name} = v }}"]
            return out

        refs = (set(used) - {name}) | image.all_class_refs()
        # NB an unannotated var compiles TWICE: the first (no-setter)
        # compile exists to RECOVER the type from the wat — and in the
        # shared-table era it is LOADED like every compile (its fragments
        # carry baked table indices; an unload would poison the IC cache).
        # The recovery module is just more (harmless) image history.
        body = body_with()
        ok, err, gen = image.compile_module(body, refs)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"

        if is_var and not annot:
            t = image.recover_val_type(gen, f"{name}__get")
            if not t:
                return False, (
                    f"cannot recover the type of '{name}' for access from later modules; "
                    "annotate it explicitly (var x: Int = ...)"
                )
            body = body_with(get_only_type=t)
            ok, err, gen = image.compile_module(body, refs)
            if not ok:
                return False, err

        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"

        old = image.bindings.get(name)
        b = {"kind": kind, "gen": gen, "kotlin_type": annot or image.recover_val_type(gen, f"{name}__get"),
             "export": f"{name}__get", "has_set": is_var}
        if kind == "val" and b["kotlin_type"] is None:
            return False, (f"defined '{name}', but its type is not re-importable in v1 "
                           "(annotate it explicitly to make it usable from later lines)")
        image.bindings[name] = b
        note = " (redefined; previous objects stay alive in the image)" if old else ""
        return True, f"defined {kind} {name}: {b['kotlin_type']}{note}"

    # ---- expression / statement thunks

    def evaluate_thunk(self, body, refs):
        image = self.image
        # NB the thunk's function name is UNIQUE PER GENERATION: the shared
        # symbol table keys bindings by name+signature, and every eval
        # thunk declaring `__eval()` would collide with the first one's
        # table index — its entry would run the FIRST thunk's body (DESIGN
        # §15). Thunks are ephemeral and never called across modules.
        body = [line.replace("__eval", f"__eval{image.gen + 1}") for line in body]
        refs = set(refs) | image.all_class_refs()
        ok, err, gen = image.compile_module(body, refs)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"
        resp = image.call(gen, f"__eval{gen}")
        if not resp.get("ok"):
            pre = (resp.get("stdout") or "").strip()
            err = f"runtime error: {resp.get('error')}"
            return False, (pre + "\n" + err if pre else err)
        out = (resp.get("stdout") or "").rstrip("\n")
        return True, out if out else "ok"

    def handle_expr_ast(self, e):
        body = ['@WasmExport("__eval")', f"fun __eval() {{ println(({e['text']})) }}"]
        return self.evaluate_thunk(body, set(e.get("used") or []))

    def handle_stmt_ast(self, b):
        body = ['@WasmExport("__eval")', f"fun __eval() {{ {b['text']} }}"]
        return self.evaluate_thunk(body, set(b.get("used") or []))


# ---------------------------------------------------------------- UI

BANNER = """\
lrepl — live-image Kotlin/Wasm REPL (each line = one wasm module).
Commands: :q :reset :list :dis NAME   (definitions have Julia semantics: new signature = new overload)
Notes: class body-only redefinitions are live; shape changes mint a new payload shape."""


def run_stdio():
    """Quiet REPL over stdin/stdout — for `M-x make-comint` (kotlin-mode's
    kotlin-send-region/buffer/block expect a *KotlinREPL* comint process):

        (make-comint "KotlinREPL" "python3" nil
                     "~/kotlin/wasm-repl-spike/lrepl/lrepl.py" "--stdio")

    Submissions are split on blank lines so pasted regions with multiple
    forms (and kotlin-send-block paragraphs) evaluate form by form."""
    repl = Lrepl()
    print("lrepl stdio — live Kotlin/Wasm image (:q quits)", flush=True)
    while True:
        try:
            line = input("lrepl> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        while not balanced(line):
            more = input("....> ")
            if not more:
                break
            line += "\n" + more
        for chunk in re.split(r'\n\s*\n', line):
            chunk = chunk.strip()
            if not chunk:
                continue
            if chunk == ":q":
                return
            try:
                ok, out = repl.handle(chunk)
            except Exception as e:  # noqa: BLE001 — a REPL must survive anything
                ok, out = False, f"internal error: {e}"
            print(out if ok else f"error:\n{out}", flush=True)


def run(lines=None):
    repl = Lrepl()
    interactive = lines is None
    print(BANNER)
    while True:
        try:
            if interactive:
                raw = input("lrepl> ")
            else:
                if not lines:
                    break
                raw = lines.pop(0)
                print(f"lrepl> {raw}")
            line = raw.strip()
            # accumulate until brackets balance (multi-line declarations)
            while not balanced(line):
                more = input("....> ") if interactive else (lines.pop(0) if lines else "")
                if not more:
                    break
                print(f"....> {more}")
                line += "\n" + more
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not line:
            continue
        t0 = time.time()
        try:
            if line == ":q":
                break
            else:
                ok, out = repl.handle(line)
        except Exception as e:  # noqa: BLE001 — a REPL must survive anything
            ok, out = False, f"internal error: {e}"
        dt = (time.time() - t0) * 1000
        print(out if ok else f"error:\n{out}")
        if ok and not line.startswith(":"):
            repl.history.append(line)
        print(f"[{dt:.0f} ms]")
    return repl


DEMO = [
    ':dis kotlin.math.sin',
    'val x = 10',
    'fun square(n: Int): Int = n * n',
    'square(x)',
    'fun square(n: Int): Int = n * n * n',
    'square(x)',
    'fun double(n: Int): Int = n + n',
    'fun scale(n: Int): Int = double(double(n))',
    'scale(5)',
    'fun double(n: Int): Int = n * 3',
    'scale(5)',
    'fun greet(name: String): String = "hello " + name',
    'fun hey(): String = greet("bob")',
    'hey()',
    'fun greet(name: String, punct: String): String = "hello " + name + punct',
    'greet("bob", "!")',
    'hey()',
    'fun greet(name: String): String = "hi " + name',
    'hey()',
    'greet("bob", "!")',
    ':list',
    'class Vec(val x: Int, val y: Int) { fun len2() = x * x + y * y }',
    'val v = Vec(3, 4)',
    'v.len2()',
    'fun desc(v: Vec): String = "(${v.x}, ${v.y})"',
    'desc(v)',
    'fun desc(v: Vec): String = "vec: ${v.x}, ${v.y}"',
    'desc(v)',
    'v.len2() + square(2)',
    'var counter = 0',
    'counter = counter + 1',
    'counter = counter + 1',
    'counter',
    'object Registry { var n = 0; fun bump() { n = n + 1 } }',
    'Registry.bump()',
    'Registry.bump()',
    'Registry.n',
    'val banner = "square and double live here"',
    'banner',
    'var k = 1',
    'fun kx2() { k = k + k }',
    'kx2()',
    'kx2()',
    'k',
    ':list',
    ':dis double',
    'import kotlin.math.abs',
    'fun showAbs(): Int = abs(-5)',
    'showAbs()',
    'fun abs(x: Int): Int = x * x',
    'showAbs()',
    'abs(-7)',
    ':dis kotlin.math.abs',
]


CLASSES_DEMO = [
    # ---- Part 1: body-only redefinition — the canonical type is IDENTICAL
    # (rec groups are content-addressed and contain only shapes; bodies are
    # not types), so old instances ARE new instances: `is` passes, and the
    # method-slot sweep makes them run the new bodies via their vtables.
    'class Vec(val x: Int, val y: Int) { fun len2() = x * x + y * y; override fun toString(): String = "Vec($x, $y)" }',
    'val v = Vec(3, 4)',
    'v.len2()',
    'println(v)',
    'v is Vec',
    'class Vec(val x: Int, val y: Int) { fun len2() = x * x + y * y + 1; override fun toString(): String = "vec<${x},${y}>" }',
    'v.len2()',
    'println(v)',
    'v is Vec',
    'val w = Vec(5, 6)',
    'println(w)',
    # ---- Part 2: field change under stable cells — the receiver type is the
    # unchanged CELL, so the engine ACCEPTS the redefinition's body swaps and
    # `is` stays true; the shape check lives at FIELD ACCESS now (the
    # ref.test against the current payload layout). Pre-cells this minted a
    # new type version and stranded every old binding.
    'class Pt(val a: Int) { fun get() = a }',
    'val p = Pt(4)',
    'p.get()',
    'val pAny: Any? = Pt(4)',
    'class Pt(val a: Int, val b: Int) { fun get() = a }',
    'Pt(4, 5).get()',
    'p.get()',          # traps: old instance holds payload v1; new code
                        # casts to payload v2 — where Stage 2 migrates lazily
    'fun isPt(x: Any?): Boolean = x is Pt',
    'isPt(pAny)',       # TRUE — identity is the cell, not the payload
    # ---- Part 3: interface shape change — the measured `is` asymmetry:
    # class `is` is structural (honest), interface `is` is RTTI/interfaceId
    # based (name-hash stable across versions -> it LIES), and casts tell
    # the truth.
    'interface Shape { fun area(): Int }',
    'class Sq(val s: Int) : Shape { override fun area() = s * s }',
    'val keep: Any? = Sq(3)',
    # adding a DEFAULT member keeps implementors source-compatible but changes
    # the itable type = a shape change for the interface
    'interface Shape { fun area(): Int; fun name(): String = "n/a" }',
    'fun isShape(x: Any?): Boolean = x is Shape',
    'isShape(keep)',
    'fun areaOf(x: Any?): Int = (x as Shape).area()',
    'areaOf(keep)',
]

HIERARCHY_DEMO = [
    # ---- overriding + super calls: a base body swap updates super-dispatch
    # but an override in the subclass correctly still wins.
    'open class Base(val n: Int) { open fun tag(): String = "base" }',
    'class Sub(n: Int) : Base(n) { override fun tag(): String = "sub:" + n }',
    'class SubSuper(n: Int) : Base(n) { override fun tag(): String = super.tag() + "!" }',
    'val s = Sub(7)',
    'val ss = SubSuper(3)',
    's.tag()',
    'ss.tag()',
    'open class Base(val n: Int) { open fun tag(): String = "BASE-v2" }',
    's.tag()',
    'ss.tag()',
    # ---- interface DEFAULT method bodies hot-swap on existing instances
    # (itables read slots, like vtables); overrides still win.
    'interface Greeter { fun hi(): String = "hello" }',
    'class G : Greeter',
    'class G2 : Greeter { override fun hi(): String = "custom" }',
    'val g = G()',
    'val g2 = G2()',
    'g.hi()',
    'g2.hi()',
    'interface Greeter { fun hi(): String = "HOWDY" }',
    'g.hi()',
    'g2.hi()',
    # ---- additive declarations are free: a NEW class flows into an OLD
    # interface-typed function (same interface rec group, canonical unification)
    'interface Animal { fun speak(): String = "..." }',
    'class Dog : Animal { override fun speak(): String = "woof" }',
    'val d = Dog()',
    'fun pat(a: Animal): String = a.speak()',
    'pat(d)',
    'class Cat : Animal { override fun speak(): String = "meow" }',
    'pat(Cat())',
    # ---- constructor changes affect only NEW instances (constructors are
    # per-module and not slotted); field/getter bodies swap normally
    'class C5(val n: Int = 5)',
    'val c1 = C5()',
    'c1.n',
    'class C5(val n: Int = 99)',
    'c1.n',
    'C5().n',
    # ---- redefinition chains sweep on top of each other
    'class D1 { fun b(): Int = 10 }',
    'val d1v = D1()',
    'd1v.b()',
    'class D1 { fun b(): Int = 20 }',
    'd1v.b()',
    'class D1 { fun b(): Int = 30 }',
    'd1v.b()',
    # ---- object redefinition: the live singleton keeps its STATE and gets
    # the new method bodies
    'object O1 { var n = 1; fun m(): Int = n }',
    'O1.n = 41',
    'O1.m()',
    'object O1 { var n = 1; fun m(): Int = n * 100 }',
    'O1.m()',
    'O1.n',
    # ---- finality is part of the shape: open -> final mints a new type
    'open class F1 { open fun t(): Int = 1 }',
    'val f1v = F1()',
    'f1v.t()',
    'class F1 { fun t(): Int = 2 }',
    'F1().t()',
    # ---- gaining an INTERFACE supertype: nothing at the type level
    # changes (interfaces erase from signatures), so the redefinition
    # LOADS — and old instances follow lazily: `is`/interface dispatch
    # installs the current RTTI + itable via the stepper and retries,
    # so a pre-existing instance answers TRUE and dispatches after
    # one migration.
    'interface An { fun name(): String = "animal" }',
    'class Plain(val v: Int)',
    'val p1 = Plain(3)',
    '(p1 as Any) is An',              # false — born before the interface (the
                                     # Any-typed check keeps the frontend from
                                     # statically refusing a provable-false)
    'class Plain(val v: Int) : An',
    'p1 is An',                       # TRUE — the stepper installed the current RTTI
    'p1.name()',                      # dispatches through the installed itable
    'Plain(4).name()',
    # ---- base SHAPE change with a frozen dependent: compile-time refusal
    # (the old subclass text no longer type-checks — honest, nothing corrupts)
    'open class B2(val a: Int) { open fun get(): Int = a }',
    'class S3(a: Int) : B2(a)',
    'val s3 = S3(1)',
    's3.get()',
    'open class B2(val a: Int, val extra: Int) { open fun get(): Int = a }',
    's3.get()',
    'S3(2).get()',
]

CELLS_DEMO = [
    # ---- Stable cells + LAYOUTS (-Xwasm-hot-reload): every class struct
    # is a CELL {vtable, itable, rtti, payload}; fields live in payload
    # LAYOUTS behind a mutable field, and field access tests the payload
    # against the class's current layout (the type system IS the check —
    # additions still subtype, deletions mint siblings). The payload TYPE
    # key is a pure function of the class — there is NO compile-time
    # history: the image's layouts table (registered at load) resolves
    # migration at RUNTIME, so old instances MIGRATE LAZILY, in place —
    # CL defclass semantics, SBCL's update-instance (DESIGN §6).
    'class Acc(var bal: Int) { fun balance(): Int = bal; fun tag(): String = "acc" }',
    'val acc = Acc(100)',
    'acc.balance()',
    'val accAny: Any? = acc',
    'fun balOf(a: Acc): Int = a.bal',  # an old-code field reader (for the deletion beat)
    # ---- field ADDITION: accepted by the engine (the receiver is the
    # unchanged cell); the new payload v2 is declared (sub v1) and new
    # instances carry it.
    'class Acc(var bal: Int, val owner: String) { fun balance(): Int = bal; fun tag(): String = "acc" }',
    'Acc(50, "ann").owner',
    'accAny is Acc',     # TRUE — identity is the cell, not the payload
    'acc.tag()',         # still runs on the old instance: reads no fields
    'fun desc(a: Acc): String = "owner=" + a.owner',
    'desc(acc)',         # LAZY MIGRATION fires here: v1 -> v2 in place,
                         # surviving bal=100 kept, owner filled with its default
    'acc.balance()',     # 100 — the surviving field survived the migration
    # ---- deletion BACK TO A PREVIOUS SHAPE: the payload text is
    # content-addressed, so it RE-UNIFIES with v1 — the instance (still
    # holding v2) passes the v1 cast as an upcast. The trap heals with no
    # migration at all.
    'class Acc(var bal: Int) { fun balance(): Int = bal; fun tag(): String = "acc" }',
    'acc.balance()',     # 100 — healed
    # ---- TRUE DELETION (a shape never seen before): no old version
    # prefixes the new field list, so the new payload is a SIBLING.
    # Migration reads doomed fields out before dropping them; afterwards,
    # OLD code touching a deleted field on the migrated instance throws
    # the honest unbound-slot (the field is gone, not shadowed).
    'class Acc(val owner: String) { fun tag(): String = "acc" }',
    'fun who(a: Acc): String = "owner=" + a.owner',
    'who(acc)',          # migrates acc: bal read out & dropped, owner kept
    'acc.tag()',         # works: reads no fields
    'balOf(acc)',       # OLD code, deleted field, migrated instance: trap
    # ---- RETYPE: same name, different type. The slot's type tag is the
    # CLASS identity the RTTI already trusts (the writer's unbox cast
    # targets exactly that class), so a retyped slot is NOT copied — a
    # copy would trap the writer's cast mid-migration and lose the WHOLE
    # instance — it takes its DEFAULT, and the instance still migrates.
    # Old code touching the retyped slot dies at the fdefn cast here
    # (the property getter's wasm signature changed — SBCL semantics;
    # a DELETED field's old binding keeps its signature, so that path
    # throws the by-name+type unbound-slot instead, as balOf above).
    'class Acc(val owner: Int) { fun tag(): String = "acc" }',
    'fun ownN(a: Acc): Int = a.owner',
    'ownN(acc)',        # migrates: owner String->Int NOT copied, defaults to 0
    'who(acc)',         # OLD code, retyped field: fdefn-cast trap
    # ---- FUNCREF SLOTS migrate too: a function-typed field is a wasm
    # funcref, which cannot cross the erased channel (the func and any
    # hierarchies are disjoint) — the value rides INSIDE an ordinary
    # stdlib box instead: the writer boxes it, the reader unboxes it,
    # and migration copies the box like any other slot value.
    'class Acc(val owner: Int, var cb: (Int) -> Int) { fun tag(): String = "acc" }',
    'acc.cb = { it + 1 }',  # migrates {owner:Int}->{owner,cb}: owner copies,
                           # then cb is WRITTEN through the box channel
    'fun callb(a: Acc, x: Int): Int = a.cb(x)',
    'callb(acc, 41)',    # 42 — fast path
    'class Acc(val owner: Int, var cb: (Int) -> Int, val note: String) { fun tag(): String = "acc" }',
    'fun noteOf(a: Acc): String = "note=" + a.note',
    'noteOf(acc)',       # migrates {owner,cb}->{owner,cb,note}: cb COPIES
                        # through reader->box->writer
    'callb(acc, 41)',   # OLD code, stale instance: a.cb read goes through
                        # slot-value -> box -> unbox -> 42
]

DEMOS = {"main": DEMO, "classes": CLASSES_DEMO, "hierarchy": HIERARCHY_DEMO, "cells": CELLS_DEMO}

if __name__ == "__main__":
    argv = sys.argv[1:]
    if "--demo" in argv:
        i = argv.index("--demo")
        name = argv[i + 1] if len(argv) > i + 1 else "main"
        demo = DEMOS.get(name)
        if demo is None:
            print(f"unknown demo '{name}' (available: {', '.join(DEMOS)})")
        else:
            print(f"=== demo: {name} ===")
            run(list(demo))
    elif "--stdio" in argv:
        run_stdio()
    else:
        run()
