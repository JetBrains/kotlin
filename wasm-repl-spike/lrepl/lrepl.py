#!/usr/bin/env python3
"""
lrepl — Lisp-like live-image REPL for Kotlin/Wasm.

Each REPL submission becomes one tiny wasm module ("generation") that
re-declares every class seen so far, @WasmImport's the previous bindings it
references from the daemon's registry, and declares only the new code.
Redefining a function swaps its slot so already-compiled callers see the new
body. No Gradle anywhere.

The driver is deliberately thin: parsing and rewriting happen in the
compile server's `parse` mode (real Kotlin PSI — see CompileServer.kt), which
returns final texts and the exact set of bindings they use. This file owns
only REPL policy (slots, semantics, registry) and the compile/daemon plumbing.

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
import time

HERE = os.path.dirname(os.path.abspath(__file__))
KOTLIN_DIST = os.environ.get("KOTLIN_DIST", os.path.join(HERE, "..", "..", "dist"))
KOTLINC_WASM = os.path.join(KOTLIN_DIST, "kotlinc", "bin", "kotlinc-wasm")
STDLIB = os.path.join(KOTLIN_DIST, "kotlinc", "lib", "kotlin-stdlib-wasm-wasi.klib")
WORK = os.environ.get("LREPL_WORK") or os.path.join(HERE, "build")
DAEMON = os.path.join(HERE, "daemon.mjs")

FILE_HEADER = "@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)"

ANSI_RE = re.compile(r'\x1b\[[0-9;]*m')


# ---------------------------------------------------------------- servers

class CompileServer:
    """Two resident JVMs from one server.jar:

    - the compile JVM runs the wasm compiler in-process per request;
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

    def _build_jar(self):
        jar = os.path.join(HERE, "server.jar")
        if os.path.exists(jar):
            return jar
        r = subprocess.run(
            [os.path.join(KOTLIN_DIST, "kotlinc", "bin", "kotlinc"),
             "-cp", os.path.join(KOTLIN_DIST, "kotlinc", "lib", "kotlin-compiler.jar"),
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
                ["java", "-cp", f"{jar}:{os.path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-compiler.jar')}",
                 "CompileServerKt"],
                stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.log, text=True,
            )
        except OSError:
            self.proc = None
            return False
        return True

    def compile(self, args):
        """(ok, diagnostics) or None → caller falls back to the CLI."""
        if not self.ensure_running():
            return None
        try:
            self.proc.stdin.write(self.US.join(args) + "\n")
            self.proc.stdin.flush()
            parts = self.proc.stdout.readline().split(" ", 2)
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
                    ["java", "-cp", f"{jar}:{os.path.join(KOTLIN_DIST, 'kotlinc', 'lib', 'kotlin-compiler.jar')}",
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


# ---------------------------------------------------------------- wasm patching

def _read_leb(buf, pos):
    result = 0
    shift = 0
    while True:
        b = buf[pos]
        pos += 1
        result |= (b & 0x7F) << shift
        if not (b & 0x80):
            return result, pos
        shift += 7


def registry_from_bytes(data):
    """The hot-reload payload-version registry custom section from wasm
    module BYTES, or None. This is the IMAGE's version table — read
    straight from the binary (no instantiation, no runtime cost; in a
    browser the same bytes come from WebAssembly.Module.customSections).
    The driver keeps the newest generation's bytes in memory, so the
    ferry never touches the filesystem."""
    try:
        sections = _split_sections(data)
    except Exception:  # noqa: BLE001 — a malformed binary has no registry
        return None
    for (sec_id, _pos, start, size) in sections:
        if sec_id != 0:
            continue
        nlen, p = _read_leb(data, start)
        name = data[p:p + nlen].decode("utf-8", "replace")
        if name == "hot-reload-registry":
            return data[p + nlen:start + size]
    return None



def _split_sections(binary):
    assert binary[:4] == b"\x00asm", "not a wasm binary"
    pos = 8
    sections = []
    while pos < len(binary):
        sec_id = binary[pos]
        size, payload_start = _read_leb(binary, pos + 1)
        sections.append((sec_id, pos, payload_start, size))
        pos = payload_start + size
    return sections


# ---------------------------------------------------------------- image

class Image:
    def __init__(self):
        self.classes = {}         # class name -> frozen declaration text (order-stable)
        self.class_refs = {}      # class name -> binding names it references
        self.bindings = {}         # name -> dict(kind, gen, sig, kotlin_type, export, slot?)
        self.gen = 0
        self.last_wasm_bytes = None   # newest generation's patched bytes (ferry + daemon)
        self.wats = {}             # gen -> wat text (for :dis and type recovery)
        self.gen_exports = {}      # gen -> export names (hot-reload slots/entries)
        self.builtin_overrides = {}  # simple name -> (donorGen, donorEntryExport)
        self.dis_probes = {}       # wasm fn name -> probe gen numbers (force-linked)
        self.daemon = None
        self.msg_id = 0
        self.server = CompileServer()
        self.ic_cache = os.environ.get("LREPL_IC", "1") == "1" and os.path.join(WORK, "ic-cache")
        self.extra_imports = set()  # import lines harvested from submitted code

    # ---- registry helpers

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
        self.wats = {}
        self.gen_exports = {}
        self.builtin_overrides = {}
        self.dis_probes = {}
        # the IC cache holds the previous compilations' fragments — the
        # payload version history. A fresh image must not chain against it
        # (fresh class declarations would bump versions against ghosts).
        if self.ic_cache and os.path.isdir(self.ic_cache):
            shutil.rmtree(self.ic_cache, ignore_errors=True)
        if self.daemon:
            self.daemon_send({"cmd": "reset"})

    # ---- compile pipeline

    def compile_module(self, body_lines, imports_needed, exclude_classes=frozenset()):
        """Assemble, klib-compile, link and patch one generation."""
        self.gen += 1
        gen = self.gen
        # bindings stranded by a shape-changing class redefinition: the engine
        # would (correctly) refuse to mix type versions at instantiation; fail
        # early with an explanation instead of an import-type error
        for n in imports_needed:
            b = self.bindings.get(n)
            if b is not None and b.get("stranded"):
                return False, (
                    f"'{n}' belongs to the OLD version of a redefined class (its type "
                    "changed), and the engine refuses to mix type versions. Re-evaluate "
                    "the binding against the new definition, or use an Any?-typed "
                    "binding to keep the old value reachable."), gen
        os.makedirs(WORK, exist_ok=True)
        # ONE evolving source path and ONE stable klib/module identity, so the
        # wasm IC cache accumulates the file's history — the previous
        # compilation's fragments (old payload structs) become a compiler
        # input, which is how the backend chains payload versions for
        # redefined classes (DESIGN §4b; no CLI args involved). Per-generation
        # names would give the cache a fresh library every line — history
        # would never accumulate.
        src_path = os.path.join(WORK, "image.kt")
        klib = os.path.join(WORK, "image.klib")
        wasm_path = os.path.join(WORK, f"gen{gen}.wasm")

        imports = self.gen_import_lines(imports_needed)
        header = [FILE_HEADER]
        header += sorted(self.extra_imports)
        # the image registry: the previous generation's payload-version
        # table, read from the LIVE newest module's custom section and
        # ferried back in as one hidden source declaration the backend
        # parses (HOTLOAD §3). The image is the previous-state store — the
        # compile does not depend on the IC cache layout.
        reg = []
        if self.gen > 1 and getattr(self, "last_wasm_bytes", None):
            # NB the LIVE image is the previous generation; its bytes are in
            # memory (compile_module) — the ferry reads the registry custom
            # section straight from them, no fs round trip
            blob = registry_from_bytes(self.last_wasm_bytes)
            if blob:
                b64 = base64.b64encode(blob).decode()
                reg = [f'internal object __hotRegistry {{ const val REGISTRY = "{b64}" }}']
        frozen = [t for n, t in self.classes.items() if n not in exclude_classes]
        source = "\n".join([*header, *reg, "", *frozen, "", *imports, "", *body_lines, ""])
        with open(src_path, "w") as f:
            f.write(source)

        ic = [f"-Xcache-directory={self.ic_cache}"] if self.ic_cache else []
        hot = ["-Xwasm-hot-reload"]  # compiler slot mode: every function dispatches
        # through a mutable slot global with an exported __hot_set$ setter
        for args in (
            ["-Xwasm-target=wasm-wasi", src_path, "-ir-output-dir", WORK,
             "-ir-output-name", "image", "-libraries", STDLIB, *hot, *ic],
            # NB the link step's -ir-output-name must be STABLE across
            # generations: it participates in the IC cache's config hash, so
            # a per-generation name gives every generation a fresh cache and
            # the payload version history never accumulates. The wasm is
            # renamed to gen{N}.wasm after the link.
            ["-Xwasm-target=wasm-wasi", f"-Xinclude={klib}", "-libraries", STDLIB,
             "-ir-output-dir", WORK, "-ir-output-name", "image-linked", "-main", "noCall", *hot, *ic],
        ):
            res = self.server.compile(args)
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
        # The gen file is a best-effort DEBUG dump (for :dis) — the runtime
        # never reads it back.
        with open(os.path.join(WORK, "image-linked.wasm"), "rb") as f:
            self.last_wasm_bytes = f.read()
        try:
            with open(wasm_path, "wb") as f:
                f.write(self.last_wasm_bytes)
        except OSError:
            pass  # debug dump only; the runtime does not need it
        return True, None, gen

    def load_module(self, gen, imports_needed):
        wasm_path = os.path.join(WORK, f"gen{gen}.wasm")
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
        # the wasm travels as bytes — the daemon never reads the fs
        wasm_b64 = base64.b64encode(self.last_wasm_bytes).decode() if getattr(self, "last_wasm_bytes", None) else None
        resp = self.daemon_send({"cmd": "load", "gen": gen, "wasmB64": wasm_b64, "wasmPath": wasm_path, "imports": host})
        if not resp.get("ok"):
            return False, resp.get("error", "instantiation failed")
        self.last_load_exports = resp.get("exports") or []
        self.gen_exports[gen] = self.last_load_exports
        # stdlib/foreign overrides: patch the NEW generation's own copies too,
        # so stdlib-internal calls in code compiled from now on see them
        for name, (donor_gen, donor_entry) in self.builtin_overrides.items():
            for exp in self.last_load_exports:
                if exp.startswith("__hot_set$") and exp[len("__hot_set$"):].split("#")[0].endswith("." + name):
                    self.swap(donor_gen, donor_entry, gen, exp)  # engine type-checks; failures ignored
        return True, None

    def call(self, gen, fn):
        return self.daemon_send({"cmd": "call", "gen": gen, "fn": fn})

    def swap(self, new_gen, new_export, slot_gen, slot_export):
        """Install the donor generation's exported ENTRY funcref into the
        slot owner's compiler-generated setter — runs entirely inside the
        daemon so opaque funcrefs never cross the JSON boundary."""
        return self.daemon_send({
            "cmd": "swapDirect", "newGen": new_gen, "newExport": new_export,
            "slotGen": slot_gen, "slotExport": slot_export,
        })

    def find_export(self, prefix, exports=None):
        """The compiler's hot-reload export names carry a reference-key hash
        suffix (overload disambiguation) — find them by prefix at load time."""
        es = exports if exports is not None else self.last_load_exports
        matches = [n for n in es if n.startswith(prefix)]
        if len(matches) != 1:
            return None
        return matches[0]

    def wat(self, gen):
        if gen not in self.wats:
            r = subprocess.run(["wasm-tools", "print", os.path.join(WORK, f"gen{gen}.wasm")],
                               capture_output=True, text=True)
            self.wats[gen] = r.stdout if r.returncode == 0 else ""
        return self.wats[gen]

    # ---- source generation helpers

    def gen_import_lines(self, needed):
        lines = []
        for name in sorted(needed):
            b = self.bindings[name]
            if b["kind"] == "fun":
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
        for l in lines:
            if l.startswith(f"  (type {token} ") or l.startswith(f"  (type {token}("):
                rm = re.search(r'\(result (.*)\)\)\)$', l)
                if not rm:
                    return None
                return self.wat_result_to_kotlin(rm.group(1))
        return None

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

def extract_dis_all(image, name, gen):
    """Disassemble every function with the given wasm name (overloads share
    names — e.g. all kotlin.io.println variants are named kotlin.io.println)."""
    wat = image.wat(gen)
    lines = wat.splitlines()
    pat = re.compile(rf'^  \(func \$"?{re.escape(name)}"? \(;')
    blocks = []
    idx = 0
    while idx < len(lines):
        if pat.match(lines[idx]):
            block = [lines[idx]]
            for l2 in lines[idx + 1:]:
                if re.match(r'^  \(', l2) or l2 == ')':
                    break
                block.append(l2)
            out = []
            tm = re.search(r' \(type (\$"[^"]*"|\$[^\s(;]+)', lines[idx])
            if tm:
                token = tm.group(1)
                for l3 in lines:
                    if l3.startswith(f"  (type {token} ") or l3.startswith(f"  (type {token}("):
                        out.append(l3)
                        break
            blocks.append("\n".join(out + block))
            idx += len(block)
        else:
            idx += 1
    return blocks


def extract_dis(image, name, gen):
    """Slice a function (and its type) out of a generation's wat text."""
    wat = image.wat(gen)
    lines = wat.splitlines()
    pat = re.compile(rf'^  \(func \$"?{re.escape(name)}"? \(;')
    for idx, l in enumerate(lines):
        if pat.match(l):
            block = [l]
            for l2 in lines[idx + 1:]:
                if re.match(r'^  \(', l2) or l2 == ')':
                    break
                block.append(l2)
            out = []
            tm = re.search(r' \(type (\$"[^"]*"|\$[^\s(;]+)', l)
            if tm:
                token = tm.group(1)
                for l3 in lines:
                    if l3.startswith(f"  (type {token} ") or l3.startswith(f"  (type {token}("):
                        out.append(l3)
                        break
            out += block
            return "\n".join(out), gen
    return None, None


# ---------------------------------------------------------------- REPL core

class Lrepl:
    def __init__(self):
        self.image = Image()
        self.image.start_daemon()
        self.history = []

    # ---- generic compile-load-register cycle

    def try_line(self, kind, build_body, register, explicit_refs=frozenset(), exclude_classes=frozenset()):
        image = self.image
        body = build_body()
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
                out.append("  patched stdlib: " + ", ".join(sorted(image.builtin_overrides)))
            return True, "\n".join(out)
        if line.startswith(":dis"):
            arg = line[4:].strip()
            return self.run_dis(arg)


        p = image.server.parse(line, image.binding_spec())
        if p is None:
            return False, "parser unavailable — the compile-server jar is missing or the parse JVM died (see build/parse-server.log)"
        return self.handle_parsed(p)

    def run_dis(self, arg):
        """Disassemble a binding (following its slot to the actual body),
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
        # DCE links only what each generation uses, so search newest to oldest.
        for gen in range(img.gen, 0, -1):
            if gen not in img.gen_exports:
                continue
            blocks = extract_dis_all(img, arg, gen)
            if blocks:
                head = f"gen {gen}: {len(blocks)} function(s) named {arg}"
                return True, head + "\n\n" + "\n\n".join(blocks)

        # not linked anywhere yet: compile a probe generation that force-links
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
        """The whole class from the newest generation's wat: the cell struct,
        its payload version structs (Stage-2 chains), the vtable type, the
        slot/RTTI globals, and every function belonging to the class — the
        constructor, methods and accessors, the hot-reload slot machinery,
        and the payload stepper/migrateFrom functions when the class has
        versions to migrate."""
        img = self.image
        klass = name if name in img.classes else f"{name}__obj"
        lines = img.wat(img.gen).splitlines()
        out = [f"class {klass} — generation {img.gen}"]

        def type_lines(pat):
            cre = re.compile(pat)
            return [l for l in lines if cre.search(l)]

        def func_blocks(namepat):
            """Top-level (func ...) blocks whose name matches namepat(tok)."""
            tok_re = re.compile(r'^  \(func \$([^ (;]+) \(;')
            blocks = []
            idx = 0
            while idx < len(lines):
                m = tok_re.match(lines[idx])
                if m and namepat(m.group(1)):
                    block = [lines[idx]]
                    for l2 in lines[idx + 1:]:
                        if re.match(r'^  \(', l2) or l2 == ')':
                            break
                        block.append(l2)
                    # prepend the function's type decl
                    tm = re.search(r' \(type (\$"[^"]*"|\$[^\s(;]+)', lines[idx])
                    if tm:
                        token = tm.group(1)
                        for l3 in lines:
                            if l3.startswith(f"  (type {token} ") or l3.startswith(f"  (type {token}("):
                                block.insert(0, l3)
                                break
                    blocks.append("\n".join(block))
                    idx += len(block) - 1
                idx += 1
            return blocks

        esc = re.escape(klass)
        # --- types: the cell, its payload versions, the vtable (nameless
        # #typeNNN <classVTable> — found via the cell's own vtable field)
        cell = type_lines(rf'^\s*\(type \${esc} \(;')
        if not cell:
            return False, f"class {klass} not linked in generation {img.gen}"
        out.append("\n  ;; cell struct (stable forever: vtable, itable, rtti, payload)")
        out.extend(cell)
        payloads = type_lines(rf'^\s*\(type \$"?#?(?:type\d+ )?{esc}\$payload')
        if payloads:
            out.append("\n  ;; payload version structs (flat field layouts; #vN chain onto older ones)")
            out.extend(payloads)
        vt = re.search(r'\(ref (\$"#type\d+ <classVTable>")\)', cell[0])
        if vt:
            vtl = type_lines(rf'^\s*\(type {re.escape(vt.group(1))} \(;')
            if vtl:
                out.append("\n  ;; vtable type")
                out.extend(vtl)
        # --- globals: the class's slots, RTTI
        glob_re = re.compile(rf'^\s*\(global \$(?:__slot\$|__hot_entry\$|__hot_set\$)?{esc}(?:[._#]|\(;)')
        # (nameless funcs excluded; slot names embed klass right after $)
        slot_globals = [l for l in lines if glob_re.match(l) and ("__slot$" in l or "_rtti" in l)]
        if slot_globals:
            out.append("\n  ;; globals: method slots + RTTI")
            out.extend(slot_globals)
        # --- member functions: constructor, methods, accessors
        members = func_blocks(lambda tok: tok.startswith(klass + "."))
        if members:
            out.append(f"\n  ;; member functions ({len(members)})")
            out.extend(members)
        # --- hot-reload payload machinery
        machinery = func_blocks(
            lambda tok: bool(re.match(rf'__(?:hot_payload|hot_migrate_from|slot|hot_entry|hot_set)\${esc}(?:[.#]|$)', tok))
        )
        if machinery:
            out.append(f"\n  ;; hot-reload machinery: slot entries/setters, payload stepper, migrateFrom ({len(machinery)})")
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

    def dis_entry_body(self, gen, entry_export):
        """The body an entry dispatches to: entry reads a slot whose init
        names the impl function; disassemble that impl in the same gen."""
        img = self.image
        wat = img.wat(gen).splitlines()
        slot_name = None
        entry_idx = next((i for i, l in enumerate(wat)
                          if l.startswith('  (func $' + entry_export + ' (;')), None)
        if entry_idx is not None:
            for l in wat[entry_idx + 1:]:
                if re.match(r'^  \(', l) or l == ')':
                    break
                m = re.match(r'\s*global\.get \$(__slot\$\S+)', l)
                if m:
                    slot_name = m.group(1)
                    break
        if not slot_name:
            return None
        impl = None
        for l in wat:
            if l.startswith('  (global $' + slot_name + ' '):
                m = re.search(r'ref\.func \$(\S+)\)', l)
                if m:
                    impl = m.group(1)
                break
        if not impl:
            return None
        body, _ = extract_dis(img, impl, gen)
        if not body:
            return None
        hdr1 = ";; dispatch: entry " + entry_export + " reads mutable slot $" + slot_name + " per call"
        hdr2 = ";; body ($" + impl + ", gen " + str(gen) + "):"
        return hdr1 + "\n" + hdr2 + "\n" + body

    def dis_fun_binding(self, name, b):
        """Show every method's body (entry -> slot -> ref.func impl)."""
        img = self.image
        outs = []
        for key, m in b.get("methods", {}).items():
            body = self.dis_entry_body(m["impl"][0], m["impl"][1])
            if body:
                outs.append(f";; method {key}\n{body}")
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
                return True, "import noted — applies to all subsequent generations"

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
            "params": params,
            "params_raw": d["paramsRaw"],
            "param_list": d["paramList"],
            "ret": d.get("ret"),
            "text": d["text"],  # whole declaration, rewritten by the parse server
            "body": d["body"],
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
          new generation's method entries unify with the old slots — swaps
          succeed and EXISTING INSTANCES run the new method bodies via their
          vtables (vtable entries read slots);
        - any shape change (fields, method signatures/set, supertypes): the
          receiver type differs — every swap is rejected, a NEW type version
          is created; old instances remain the old type (coexistence), new
          instances are the new type. Reported honestly either way."""
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
            swaps = self.patch_class_method_slots(name, gen)
            image.classes[name] = new_text
            image.class_refs[name] = used
            if swaps:
                methods = sorted({seg for _, seg in swaps})
                gens = len({g for g, _ in swaps})
                return (f"redefined class {name} — body swaps applied to "
                        f"{', '.join(methods)} across {gens} generation(s); "
                        "existing instances now run the new method bodies")
            report = (f"redefined class {name} — its TYPE changed (fields, method "
                      "signatures or supertypes), so this is a new type version: existing "
                      "instances remain the old type (engine-verified); new instances "
                      "are the new type")
            stranded = sorted(
                n for n, b in image.bindings.items()
                if n != name and not b.get("stranded") and self.binding_mentions(b, name)
            )
            for n in stranded:
                image.bindings[n]["stranded"] = True
            if stranded:
                report += ("\nold-version bindings (unreachable from new code, engine-"
                            "enforced): " + ", ".join(stranded))
            return report

        body = [new_text]
        exclude = frozenset({name}) if redefinition else frozenset()
        return self.try_line("class", lambda: body, register, explicit_refs=used,
                             exclude_classes=exclude)

    def binding_mentions(self, b, cls):
        """Nominal check: does a binding's recorded type/signature text name the
        class? (Registry bookkeeping only — the engine remains the judge.)"""
        if b.get("kind") in ("val", "var", "obj"):
            return b.get("kotlin_type") == cls
        if b.get("kind") == "fun":
            return bool(re.search(rf"\b{re.escape(cls)}\b", b.get("sig") or ""))
        return False

    def patch_class_method_slots(self, klass, donor_gen):
        """Engine-judged method-body swap across every live generation.

        Nominal matching only: pair each older generation's
        `__hot_set$<Class>.<method>#` export with the donor generation's
        `__hot_entry$<Class>.<method>#` by name segment, and let the engine's
        setter type check decide — a method's wasm type includes the receiver
        `(ref null $Class)`, so a swap succeeds iff the class shape is
        unchanged. No parsing, no diffing, no field extraction."""
        image = self.image
        donor_exports = image.gen_exports.get(donor_gen, [])
        donor_entries = {}
        for e in donor_exports:
            if not e.startswith("__hot_entry$"):
                continue
            seg = e[len("__hot_entry$"):e.rindex("#")]
            if seg.startswith(klass + "."):
                donor_entries[seg] = e
        if not donor_entries:
            return []
        swaps = []
        for g, exports in list(image.gen_exports.items()):
            if g == donor_gen:
                continue
            for e in exports:
                if not e.startswith("__hot_set$"):
                    continue
                seg = e[len("__hot_set$"):e.rindex("#")]
                entry = donor_entries.get(seg)
                if entry is None:
                    continue
                resp = image.swap(donor_gen, entry, g, e)
                if resp.get("ok"):
                    swaps.append((g, seg))
        return swaps

    def handle_object_ast(self, d):
        """Object redefinition = class redefinition of <name>__obj with the
        binding's accessor kept stable: the singleton the registry points at
        is the OLD generation's instance, so the method-slot sweep is what
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
                    swaps = self.patch_class_method_slots(klass, gen)
                    existing["stranded"] = False
                    if swaps:
                        methods = sorted({seg.split(".", 1)[1] for _, seg in swaps})
                        return (f"redefined object {name} — body swaps applied to "
                                f"{', '.join(methods)} across {len({g for g, _ in swaps})} "
                                "generation(s); the live singleton now runs the new bodies")
                    return (f"redefined object {name} — its TYPE changed, so a new type "
                            "version was minted; the live singleton remains the old type")
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
        return self.try_line("obj", lambda: body, register, explicit_refs=used,
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

        ok, err, gen = image.compile_module([decl_text], refs)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"
        new_entry = image.find_export(f"__hot_entry${name}#")
        new_setter = image.find_export(f"__hot_set${name}#")
        if not (new_entry and new_setter):
            return False, f"hot-reload exports not found for {name}"

        method = {"gen": gen, "setter": new_setter, "export": new_entry,
                  "fn_type": fn_type, "sig": sig, "impl": (gen, new_entry)}

        if existing is not None and fn_type in existing["methods"]:
            # ---- REPLACE the method: swap the ORIGINAL generation's slot, so
            # every caller — old and new — funnels through the one slot.
            m = existing["methods"][fn_type]
            resp = image.swap(gen, new_entry, m["gen"], m["setter"])
            if not resp.get("ok"):
                return False, (f"slot swap rejected (textual signatures matched but wasm "
                               f"types differ): {resp.get('error')}")
            m["impl"] = (gen, new_entry)
            return True, (f"redefined fun {name}{fn_type} — live slot swapped; "
                         "existing callers now run the new body")

        if existing is None:
            # ---- first definition of the name (possibly shadowing a stdlib
            # slot — patch_builtin_slots sweeps the image for matches)
            existing = {"kind": "fun", "methods": {}}
            image.bindings[name] = existing
            patched = self.patch_builtin_slots(name, gen, new_entry)
            note = ""
            if patched:
                targets = ", ".join(sorted({fq for _, fq, _ in patched}))
                note = (f"; patched {len(patched)} slot(s) of preexisting {targets} "
                        f"across {len({g for g, _, _ in patched})} generation(s) — "
                        "already-compiled call sites now run the new body")
            existing["methods"][fn_type] = method
            return True, f"defined fun {name}{fn_type}{note}"

        # ---- ADD a method (Julia: new method of the generic function).
        existing["methods"][fn_type] = method
        others = ", ".join(k for k in existing["methods"] if k != fn_type)
        return True, (f"defined fun {name}{fn_type} — new overload of {name} "
                      + (f"(existing: {others})" if others else "")
                      + "; existing methods and their callers are untouched")

    def patch_builtin_slots(self, name, donor_gen, donor_entry):
        """Install the donor entry into every generation's matching stdlib
        slot (dotted names only — REPL user functions live in the default
        package). The engine rejects setters whose wasm type doesn't match,
        which is how the right overload is chosen. Returns the swaps that
        took; registers an override so future generations get patched too."""
        image = self.image
        patched = []
        for g, exports in list(image.gen_exports.items()):
            for exp in exports:
                if not exp.startswith("__hot_set$"):
                    continue
                fq = exp[len("__hot_set$"):].split("#")[0]
                if "." not in fq or fq.split(".")[-1] != name:
                    continue
                resp = image.swap(donor_gen, donor_entry, g, exp)
                if resp.get("ok"):
                    patched.append((g, fq, exp))
        if patched:
            image.builtin_overrides[name] = (donor_gen, donor_entry)
        return patched

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
        body = body_with()
        ok, err, gen = image.compile_module(body, refs)
        if not ok:
            return False, err

        if is_var and not annot:
            t = image.recover_val_type(gen, f"{name}__get")
            if not t:
                return False, (
                    f"cannot recover the type of '{name}' for cross-generation access; "
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
        b["used"] = {r for r in used if r in image.bindings and r != name}
        image.bindings[name] = b
        note = " (redefined; previous objects stay alive in the image)" if old else ""
        return True, f"defined {kind} {name}: {b['kotlin_type']}{note}"

    # ---- expression / statement thunks

    def evaluate_thunk(self, body, refs):
        image = self.image
        refs = set(refs) | image.all_class_refs()
        ok, err, gen = image.compile_module(body, refs)
        if not ok:
            return False, err
        ok, err = image.load_module(gen, refs)
        if not ok:
            return False, f"instantiation failed: {err}"
        resp = image.call(gen, "__eval")
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
lrepl — live-image Kotlin/Wasm REPL (each line = one wasm generation).
Commands: :q :reset :list :dis NAME   (definitions have Julia semantics: new signature = new overload)
Notes: class body-only redefinitions are live; shape changes mint a new type version."""


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
        while not balanced(line) or (line.endswith(("=", "{", "(")) and not balanced(line)):
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
            while not balanced(line) or (line.endswith(("=", "{", "(")) and not balanced(line)):
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
    # `is` stays true; the version check lives at FIELD ACCESS now (the
    # cast to the current payload). Pre-cells this minted a new type version
    # and stranded every old binding.
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
    # per-generation and not slotted); field/getter bodies swap normally
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
    # ---- gaining a supertype = shape change; new instances pass `is`
    'interface An { fun name(): String = "animal" }',
    'class Plain(val v: Int)',
    'class Plain(val v: Int) : An',
    'Plain(3).name()',
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
    # ---- Stable cells + payload versions (-Xwasm-hot-reload): every class
    # struct is a CELL {vtable, itable, rtti, payload}; fields live in
    # per-class payload structs behind a mutable field, and field access
    # carries a version check. The previous compilation's fragments (the IC
    # channel) give the compiler the class's payload history, so a
    # redefinition chains a new version and old instances MIGRATE LAZILY, in
    # place — CL defclass semantics (DESIGN §4b).
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
    # OLD code touching a deleted field on the migrated instance fails its
    # version cast — the invalidation trampoline (the field is gone, not
    # shadowed).
    'class Acc(val owner: String) { fun tag(): String = "acc" }',
    'fun who(a: Acc): String = "owner=" + a.owner',
    'who(acc)',          # migrates acc: bal read out & dropped, owner kept
    'acc.tag()',         # works: reads no fields
    'balOf(acc)',       # OLD code, deleted field, migrated instance: trap
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
