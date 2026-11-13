#!/usr/bin/env python3
"""
swank.py — a minimal SLIME backend (swank protocol) on top of the lrepl
Kotlin/Wasm live image.

Run:    python3 swank.py [port]        (default 4005)
Emacs:  M-x slime-connect RET localhost RET 4005

Protocol notes: swank frames are `<6-hex-length><sexp>`; requests arrive as
(:emacs-rex "LISP-CODE" PACKAGE THREAD ID ARGS). We implement the subset of
swank operations needed for interactive development — connection-info, ping,
interactive-eval (C-x C-e), listener-eval (REPL), compile-string-for-emacs
(C-c C-c), completions (M-TAB), describe-symbol (C-c C-d d),
find-definitions-for-emacs (M-.), the SLDB debugger protocol (runtime traps
pop up an sldb buffer: :debug / :debug-activate / debugger-info-for-emacs /
throw-to-toplevel), create-repl, quit. Output produced by evaluated Kotlin
(captured WASI stdout) is streamed back via :write-string.

Caveat: exercised against a scripted fake-slime client (swank_test.py);
real SLIME versions vary in what connection-info fields they require, so
minor tweaks may be needed for your slime version.
"""

import os
import re
import socket
import sys

from lrepl import Lrepl, WORK

KW, SYM = "kw", "sym"


# ---------------------------------------------------------------- source scan

def enclosing_declaration(text, position):
    """The top-level Kotlin declaration containing `position` (a char
    offset), or None. C-c C-c has no Kotlin notion of "defun at point": slime
    sends whatever region its Lisp heuristics produce, so the server instead
    re-reads the file slime named and submits the whole enclosing declaration
    — a member-at-point re-evaluates its class, a top-level function itself.
    Annotations on their own lines belong to the declaration below them."""
    # blank out comments and strings, preserving offsets
    cleaned = list(text)
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            while i < n and text[i] != '\n':
                cleaned[i] = ' '
                i += 1
        elif c == '/' and i + 1 < n and text[i + 1] == '*':
            cleaned[i] = cleaned[i + 1] = ' '
            i += 2
            while i < n:
                if text[i] == '*' and i + 1 < n and text[i + 1] == '/':
                    cleaned[i] = cleaned[i + 1] = ' '
                    i += 2
                    break
                if text[i] != '\n':
                    cleaned[i] = ' '
                i += 1
        elif c == '"':
            cleaned[i] = ' '
            i += 1
            while i < n and text[i] != '"':
                if text[i] == '\\' and i + 1 < n:
                    cleaned[i] = cleaned[i + 1] = ' '
                    i += 1
                if i < n and text[i] != '\n':
                    cleaned[i] = ' '
                i += 1
        else:
            i += 1
    cleaned = ''.join(cleaned)

    # depth-0 declaration headers and their spans
    headers = []  # (start_offset, header_is_annotation)
    depth = 0
    i = 0
    while i < n:
        if cleaned[i] == '{':
            depth += 1
        elif cleaned[i] == '}':
            depth -= 1
        elif cleaned[i] == '\n' and depth == 0:
            line_start = i + 1
            line = cleaned[line_start:cleaned.find('\n', line_start) if '\n' in cleaned[line_start:] else n]
            stripped = line.strip()
            if stripped.startswith('@'):
                headers.append((line_start + line.index(stripped[0]), True))
            elif re.match(r'(class|object|interface|fun|val|var|typealias)\b', stripped):
                headers.append((line_start + line.index(stripped[0]), False))
        i += 1

    if not headers:
        return None
    # a declaration starts at its first preceding annotation run at depth 0
    starts = []
    for idx, (off, is_annot) in enumerate(headers):
        start = off
        j = idx - 1
        while j >= 0 and headers[j][1]:  # annotations directly above attach
            start = headers[j][0]
            j -= 1
        starts.append(start)
    spans = []
    for k, start in enumerate(starts):
        end = starts[k + 1] if k + 1 < len(starts) else n
        spans.append((start, end))
    for start, end in spans:
        if start <= position <= end:
            return text[start:end].strip()
    return None


# ---------------------------------------------------------------- sexp codec

def read_sexp(s, i=0):
    """Parse one s-expression from s[i:]. Returns (value, next_index)."""
    if not isinstance(s, str):
        return None, 0
    n = len(s)
    while i < n and s[i] in " \t\r\n":
        i += 1
    if i >= n:
        return None, i
    c = s[i]
    if c == '(':
        i += 1
        out = []
        while True:
            while i < n and s[i] in " \t\r\n":
                i += 1
            if i >= n:
                return out, i
            if s[i] == ')':
                return out, i + 1
            v, i = read_sexp(s, i)
            out.append(v)
    if c == "'":
        v, i = read_sexp(s, i + 1)
        return [(SYM, "quote"), v], i
    if c == '"':
        i += 1
        buf = []
        while i < n and s[i] != '"':
            if s[i] == '\\' and i + 1 < n:
                buf.append(s[i + 1])
                i += 2
            else:
                buf.append(s[i])
                i += 1
        return ''.join(buf), i + 1
    m = re.match(r'[^()\s"]+', s[i:])
    if not m:
        return None, i
    tok = m.group(0)
    i += len(tok)
    if tok == '.':  # dotted pair: (a . b)
        v, i = read_sexp(s, i)
        return v, i  # represent the pair tail inline; callers treat lists loosely
    if tok in ('nil', 'NIL'):
        return [], i
    if tok in ('t', 'T'):
        return True, i
    if re.fullmatch(r'-?\d+', tok):
        return int(tok), i
    if tok.startswith(':'):
        return (KW, tok.lower()), i
    return (SYM, tok), i


def write_sexp(o):
    if o is None or o is False or o == []:
        return 'nil'
    if o is True:
        return 't'
    if isinstance(o, list):
        return '(' + ' '.join(write_sexp(x) for x in o) + ')'
    if isinstance(o, tuple):
        if o[0] in (KW, SYM):
            return o[1]
        return write_sexp(list(o))
    if isinstance(o, str):
        return '"' + o.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'
    if isinstance(o, bool):
        return 't' if o else 'nil'
    if isinstance(o, (int, float)):
        return str(o)
    raise TypeError(f"cannot serialize {o!r}")


def autodoc_operator(context):
    """The innermost form containing the cursor marker; its first token is
    the operator slime is asking about. Falls back to the deepest form."""
    marker = "swank::%cursor-marker%"

    def is_marker(m):
        # the marker arrives either as a plain string or as a parsed symbol
        return m == marker or (isinstance(m, tuple) and m == (SYM, marker))

    def innermost(node):
        if not isinstance(node, list):
            return None
        has_marker = any(is_marker(m) for m in node)
        if has_marker:
            # marker directly in this form — but a nested form may be deeper
            for item in node:
                deeper = innermost(item)
                if deeper is not None:
                    return deeper
            return node
        for item in node:
            deeper = innermost(item)
            if deeper is not None:
                return deeper
        return None

    form = innermost(context) if context else None
    if form and form and isinstance(form[0], str):
        return form[0]
    return None


# ---------------------------------------------------------------- server

class SwankResponse:
    def __init__(self, return_value=None, events=(), close=False, debug_condition=None):
        self.return_value = return_value
        self.events = list(events)  # pre-rendered sexp strings, e.g. '(:write-string "x")'
        self.close = close
        # Runtime traps with this condition attached open an SLDB popup
        # (the :debug event flow) instead of returning right away; the
        # original rex is answered (:abort ...) when the debugger quits.
        self.debug_condition = debug_condition
        self.ident = 0


class SwankServer:
    def __init__(self, port):
        self.port = port
        self.repl = Lrepl()
        self.protocol_version = "2.30"
        self.trace = True  # log frames to stderr for protocol debugging
        self.active_debug = None  # condition text while an sldb popup is open

    def log(self, direction, text):
        if self.trace:
            print(f"; swank {direction}: {text[:200]}", flush=True)

    # -- framing

    @staticmethod
    def send_frame(f, sexp_text):
        payload = sexp_text.encode()
        f.write(f"{len(payload):06x}".encode() + payload)
        f.flush()

    # -- swank operations

    def eval_string(self, code):
        """Evaluate a Kotlin submission in the live image."""
        code = code.strip()
        if not code:
            return True, ""
        try:
            return self.repl.handle(code)
        except Exception as e:  # noqa: BLE001 — a swank error is a response, not a crash
            return False, f"internal error: {e}"

    @staticmethod
    def is_runtime_error(out):
        """Runtime wasm traps (illegal cast, uncaught exceptions) reach the
        daemon as engine errors; compile errors are frontend text. Only the
        former belong in an SLDB popup — compile errors are already reported
        inline / as compilation notes."""
        return bool(out) and out.startswith("runtime error:")

    @staticmethod
    def pretty_condition(out):
        """Make the sldb condition honest about the one payload we cannot
        read: wasm exception tags are module-local, so an uncaught Kotlin
        exception arrives as an opaque WebAssembly.Exception from the
        daemon (ROADMAP §5 (exception tag export) — export the tag and this becomes a message)."""
        if "[object WebAssembly.Exception]" in out:
            return ("uncaught Kotlin exception — wasm exception tags are "
                    "module-local, so the payload is not readable here; wrap the "
                    "call in try/catch and println it (see ROADMAP: export the exception tag)")
        return out

    def op_connection_info(self, args):
        info = [
            (KW, ":pid"), os.getpid(),
            (KW, ":dir"), os.getcwd(),
            (KW, ":machine-instance"), "lrepl",
            (KW, ":machine-type"), "wasm",
            (KW, ":machine-version"), "wasm-gc",
            (KW, ":lisp-implementation"), [(KW, ":name"), "lrepl",
                                            (KW, ":type"), "Kotlin/Wasm live image",
                                            (KW, ":version"), "1.0"],
            (KW, ":package"), [(KW, ":name"), "lrepl", (KW, ":prompt"), "lrepl> "],
            (KW, ":version"), self.protocol_version,
            (KW, ":features"), [(KW, ":kotlin"), (KW, ":wasm"), (KW, ":lrepl-image")],
            (KW, ":modules"), [],
        ]
        return SwankResponse(info)

    def op_ping(self, args):
        return SwankResponse([(KW, ":pong")])

    def op_interactive_eval(self, args):
        code = args[0] if args else ""
        ok, out = self.eval_string(code)
        if not ok and self.is_runtime_error(out):
            return SwankResponse(f"=> {out}", debug_condition=out)
        text = out if out else ("ok" if ok else "")
        if not ok:
            text = "error: " + text
        return SwankResponse(f"=> {text}")

    def op_listener_eval(self, args):
        code = args[0] if args else ""
        ok, out = self.eval_string(code)
        if not ok and self.is_runtime_error(out):
            return SwankResponse(
                [(KW, ":values"), out],
                events=[f"(:write-string {write_sexp(out + chr(10))})"],
                debug_condition=out,
            )
        text = out if out else ("ok" if ok else "")
        if not ok:
            text = "error: " + text
        return SwankResponse(
            [(KW, ":values"), text],
            events=[f"(:write-string {write_sexp(text + chr(10))})"],
        )

    def op_compile_string(self, args):
        """C-c C-c: (swank:compile-string-for-emacs STRING BUFFER POSITION
        FILENAME POLICY). The result plist is destructured by
        slime-compilation-finished as (:compilation-result NOTES SUCCESSP
        DURATION LOADP FASLFILE); note locations must be (:error MSG) or
        (:location SOURCE POS HINTS) — slime-dcase rejects anything else."""
        code = args[0] if args and isinstance(args[0], str) else ""
        buffer_name = args[1] if len(args) > 1 and isinstance(args[1], str) else ""
        position = None
        pos_form = args[2] if len(args) > 2 and isinstance(args[2], list) else []
        if pos_form and pos_form[0] == (SYM, "quote"):
            pos_form = pos_form[1] if len(pos_form) > 1 else []
        for item in pos_form if isinstance(pos_form, list) else []:
            if isinstance(item, list) and item and item[0] == (KW, ":position") and len(item) > 1:
                position = item[1]
        # Regions sent by C-c C-c exclude the file header; harvest the
        # buffer's import lines from the file slime names so the code
        # resolves the same names it would inside that file.
        filename = args[3] if len(args) > 3 and isinstance(args[3], str) else None
        if filename and os.path.exists(filename):
            try:
                with open(filename) as f:
                    file_text = f.read()
                # C-c C-c that Just Works: Kotlin has no defuns, so whatever
                # region slime's Lisp heuristics grabbed, re-submit the whole
                # top-level declaration containing point — a member-at-point
                # re-evaluates its enclosing class.
                if position:
                    decl = enclosing_declaration(file_text, position)
                    if decl:
                        code = decl
                p = self.repl.image.server.parse(file_text, self.repl.image.binding_spec())
                if p and p.get("imports"):
                    self.repl.image.extra_imports.update(f"import {i}" for i in p["imports"])
            except OSError:
                pass
        ok, out = self.eval_string(code)
        if not ok and self.is_runtime_error(out):
            return SwankResponse([(KW, ":compilation-result"), [], False, 0.0, False, False],
                                 debug_condition=out)
        notes = []
        if not ok:
            if buffer_name:
                loc = [(KW, ":location"),
                       [(KW, ":buffer"), buffer_name, (KW, ":position"), position or 0],
                       [], []]
            else:
                loc = [(KW, ":error"), "no source location"]
            notes = [[(KW, ":message"), out,
                      (KW, ":severity"), (KW, ":error"),
                      (KW, ":location"), loc]]
        result = [(KW, ":compilation-result"), notes, ok, 0.0, False, False]
        events = [f"(:write-string {write_sexp(out + chr(10))})"] if (ok and out) else []
        return SwankResponse(result, events=events)

    # -- SLDB: the debugger protocol (pretty exception popups). A runtime
    # trap during listener-eval / interactive-eval / compile-string opens an
    # sldb buffer: we push :debug + :debug-activate and hold the original
    # rex's reply until the user quits the debugger, then answer it
    # (:abort <condition>) — exactly what swank.lisp's sldb-loop does, minus
    # the Lisp restart machinery. Restarts are our two exit paths; frames are
    # the two we can honestly name (wasm stacks are engine-internal; :dis is
    # the real backtrace story).

    def debugger_payload(self, condition):
        cond = [self.pretty_condition(condition), "   [Condition of type Kotlin/Wasm runtime trap]"]
        restarts = [["ABORT", "Return to the lrepl image toplevel."],
                    ["THROW", "Throw to top level."]]
        frames = [
            [0, "lrepl image — evaluation of the last submission", [(KW, ":restartable"), False]],
            [1, "wasm trap (stack frames are engine-internal; try :dis NAME)"],
        ]
        return cond, restarts, frames, []

    def op_debugger_info(self, args):
        cond, restarts, frames, conts = self.debugger_payload(self.active_debug or "unknown trap")
        return SwankResponse([cond, restarts, frames, conts])

    def op_backtrace(self, args):
        _, _, frames, _ = self.debugger_payload(self.active_debug or "unknown trap")
        start = args[0] if args and isinstance(args[0], int) else 0
        end = args[1] if len(args) > 1 and isinstance(args[1], int) else len(frames)
        return SwankResponse(frames[start:end])

    def op_throw_toplevel(self, args):
        """q / the THROW restart: also the reply that ends the dialog."""
        return SwankResponse([(KW, ":abort"), "toplevel"])

    def op_sldb_abort(self, args):
        return SwankResponse([(KW, ":abort"), "toplevel"])

    def op_invoke_nth_restart(self, args):
        # both restarts are toplevel exits for an image-based REPL
        return SwankResponse([(KW, ":abort"), "toplevel"])

    # -- introspection: describe (C-c C-d d) and find-definitions (M-.)

    def describe_symbol(self, name):
        image = self.repl.image
        lines = []
        b = image.bindings.get(name)
        if b:
            if b.get("kind") == "fun":
                lines.append(f"{name} is a function (Julia-style generic).")
                for key, m in sorted(b["methods"].items()):
                    lines.append(f"  method {m['sig']}  [defined in generation {m['gen']}]")
                lines.append("A same-signature definition swaps the live slot; every")
                lines.append("already-compiled caller runs the new body immediately.")
            elif b.get("kind") == "obj":
                lines.append(f"{name} is an object; its live singleton (created in")
                lines.append(f"generation {b['gen']}) is shared across the image;")
                lines.append("redefinitions update its method bodies and keep its state.")
            else:
                lines.append(f"{name} is a {b['kind']} of type {b.get('kotlin_type')}")
                lines.append(f"  (defined in generation {b['gen']}; state is shared across the image).")
        if name in image.classes:
            lines.append(f"{name} is a class. Current definition (generation {image.gen}):")
            for ln in image.classes[name].splitlines():
                lines.append("  " + ln)
        if not lines:
            lines.append(f"{name} is not defined in the image.")
            cands = sorted(n for n in (*image.bindings, *image.classes) if name.lower() in n.lower())[:12]
            if cands:
                lines.append(f"Image names containing '{name}': {', '.join(cands)}")
        return "\n".join(lines)

    def op_describe_symbol(self, args):
        return SwankResponse(self.describe_symbol(args[0] if args and isinstance(args[0], str) else ""))

    def op_documentation_symbol(self, args):
        return SwankResponse(self.describe_symbol(args[0] if args and isinstance(args[0], str) else ""))

    def op_find_definitions(self, args):
        """M-.: locate the defining generation file. Every submission is
        written to build/image.kt (one evolving source path), so the image's record of a binding (its
        defining generation) or a class (its current text) pins the file."""
        name = args[0] if args and isinstance(args[0], str) else ""
        image = self.repl.image

        def locate(gen, needle):
            # the current world lives in build/image.kt (one evolving source
            # path); older gens' files are history
            for path in (os.path.join(WORK, "image.kt"), os.path.join(WORK, f"gen{gen}.kt")):
                try:
                    with open(path) as fh:
                        text = fh.read()
                except OSError:
                    continue
                off = text.find(needle)
                if off >= 0:
                    return [(KW, ":location"), [(KW, ":file"), path],
                            [(KW, ":position"), off + 1], []]
            return None

        results = []
        b = image.bindings.get(name)
        if b:
            if b.get("kind") == "fun":
                for key, m in sorted(b["methods"].items()):
                    loc = locate(m["gen"], f"fun {name}(")
                    if loc:
                        results.append([m["sig"], loc])
            else:
                kw = {"obj": "class", "val": "val", "var": "var"}.get(b["kind"], "val")
                loc = locate(b["gen"], f"{kw} {name}")
                if loc:
                    results.append([name, loc])
        if name in image.classes:
            for gen in range(image.gen, 0, -1):
                loc = locate(gen, image.classes[name])
                if loc:
                    results.append([f"class {name}", loc])
                    break
        if not results:
            return SwankResponse([(KW, ":error"), f"no definition of '{name}' in the image"])
        return SwankResponse(results)

    def op_autodoc(self, args):
        """swank:autodoc (context) [:print-right-margin N] — slime's eldoc.

        The context is the cursor's enclosing form as a list of source
        tokens (with %cursor-marker% where point is). The protocol result
        must be a LIST: (docstring cache-p) or (:not-available) — returning
        nil crashes slime's destructuring-bind with "Wrong number of
        arguments". We return the signature when the innermost form's
        operator names a REPL binding (or class), else :not-available."""
        ctx = args[0] if args else None
        if isinstance(ctx, list) and ctx and ctx[0] == (SYM, "quote"):
            ctx = ctx[1] if len(ctx) > 1 else []
        head = autodoc_operator(ctx)
        if head:
            image = self.repl.image
            b = image.bindings.get(head)
            if b:
                if b.get("kind") == "fun":
                    doc = "  |  ".join(m["sig"] for m in b["methods"].values())
                else:
                    doc = f"{b['kind']} {head}: {b.get('kotlin_type')}"
                return SwankResponse([doc, True])
            if head in image.classes:
                return SwankResponse([f"class {head}", True])
        return SwankResponse([(KW, ":not-available")])

    def op_completions(self, args):
        prefix = args[0] if args and isinstance(args[0], str) else ""
        names = sorted(n for n in self.repl.image.bindings if n.startswith(prefix))
        return SwankResponse(names)

    def op_simple_completions(self, args):
        """swank:simple-completions returns (completions common-prefix)."""
        prefix = args[0] if args and isinstance(args[0], str) else ""
        names = sorted(n for n in self.repl.image.bindings if n.startswith(prefix))
        if names:
            common = names[0]
            for nm in names[1:]:
                while not nm.startswith(common):
                    common = common[:-1]
        else:
            common = prefix
        return SwankResponse([names, common])

    def op_create_repl(self, args):
        """swank-repl:create-repl destructures the result as (PACKAGE PROMPT)."""
        return SwankResponse(["lrepl", "lrepl> "])

    def op_swank_require(self, args):
        """swank:swank-require — slime stores the response as its loaded module
        list; echo back what was requested (we accept all contribs)."""
        requested = args[0] if args else []
        if isinstance(requested, list) and requested and requested[0] == (SYM, "quote"):
            requested = requested[1] if len(requested) > 1 else []
        if not isinstance(requested, list):
            requested = []
        return SwankResponse(requested)

    def op_quit(self, args):
        return SwankResponse([], close=True)

    def dispatch(self, form):
        if not isinstance(form, list) or not form or form[0] != (KW, ":emacs-rex"):
            return SwankResponse([], close=True)
        code, package, thread, ident = form[1], form[2], form[3], form[4]
        extras = form[5] if len(form) > 5 else []
        # Real slime sends the form as a nested list; some clients send a string.
        if isinstance(code, list):
            call = code
        elif isinstance(code, str):
            call, _ = read_sexp(code)
        else:
            return SwankResponse([])
        if not (isinstance(call, list) and call and isinstance(call[0], tuple)):
            return SwankResponse([])

        # Slime announces its protocol version in the first request's extras;
        # echoing it avoids the version-mismatch connection abort.
        flat = extras if isinstance(extras, list) else []
        for j, item in enumerate(flat):
            if item == (KW, ":protocol-version") and j + 1 < len(flat) and isinstance(flat[j + 1], str):
                self.protocol_version = flat[j + 1]

        op_name = call[0][1]
        args = call[1:]
        table = {
            "swank:connection-info": self.op_connection_info,
            "swank:ping": self.op_ping,
            "swank:interactive-eval": self.op_interactive_eval,
            "swank:listener-eval": self.op_listener_eval,
            "swank-repl:listener-eval": self.op_listener_eval,
            "swank:compile-string-for-emacs": self.op_compile_string,
            "swank:completions": self.op_completions,
            "swank:simple-completions": self.op_simple_completions,
            "swank:autodoc": self.op_autodoc,
            "swank:completions-for-keyword": self.op_completions,
            "swank:create-repl": self.op_create_repl,
            "swank-repl:create-repl": self.op_create_repl,
            "swank:create-listener": self.op_create_repl,
            "swank:swank-require": self.op_swank_require,
            "swank:debugger-info-for-emacs": self.op_debugger_info,
            "swank:backtrace": self.op_backtrace,
            "swank:throw-to-toplevel": self.op_throw_toplevel,
            "swank:sldb-abort": self.op_sldb_abort,
            "swank:invoke-nth-restart": self.op_invoke_nth_restart,
            "swank:describe-symbol": self.op_describe_symbol,
            "swank:documentation-symbol": self.op_documentation_symbol,
            "swank:find-definitions-for-emacs": self.op_find_definitions,
            "swank:quit-lisp": self.op_quit,
            "swank:quit-connection": self.op_quit,
        }
        handler = table.get(op_name)
        try:
            resp = handler(args) if handler else SwankResponse([])
            if not isinstance(resp, SwankResponse):
                resp = SwankResponse(resp)
        except Exception as e:  # noqa: BLE001 — a bad op must never kill the connection
            self.log("err", f"{op_name}: {e!r}")
            resp = SwankResponse([(KW, ":error"), f"lrepl swank: {e}"])
        resp.ident = ident
        return resp

    def debugger_dialog(self, f, resp):
        """The sldb popup: replay of swank.lisp's sldb-loop. Sends :debug +
        :debug-activate, then serves the debugger RPCs (info, backtrace,
        describe, completions — the image stays usable) until a toplevel exit
        (q / a / restart invocation), then answers the ORIGINAL rex with
        (:abort <condition>) and :debug-return so slime closes the buffer."""
        cond, restarts, frames, conts = self.debugger_payload(resp.debug_condition)
        self.active_debug = resp.debug_condition
        self.send_frame(f, f"(:debug 1 1 {write_sexp(cond)} {write_sexp(restarts)} "
                           f"{write_sexp(frames)} {write_sexp(conts)})")
        self.send_frame(f, "(:debug-activate 1 1 t)")
        try:
            while True:
                header = f.read(6)
                if not header or len(header) < 6:
                    return
                data = f.read(int(header.decode(), 16)).decode()
                self.log("req", data)
                form, _ = read_sexp(data)
                inner = self.dispatch(form)
                for event in inner.events:
                    self.send_frame(f, event)
                if inner.close:
                    self.send_frame(f, f"(:return (:ok nil) {write_sexp(inner.ident)})")
                    return
                if inner.return_value == [(KW, ":abort"), "toplevel"]:
                    # the debugger is quitting: ack this rex, close the popup,
                    # and abort the rex that trapped
                    self.send_frame(f, f"(:return (:ok {write_sexp(inner.return_value)}) {write_sexp(inner.ident)})")
                    self.send_frame(f, "(:debug-return 1 1 nil)")
                    abort = f"(:return (:abort {write_sexp(resp.debug_condition)}) {write_sexp(resp.ident)})"
                    self.send_frame(f, abort)
                    self.log("rep", abort)
                    return
                reply = f"(:return (:ok {write_sexp(inner.return_value)}) {write_sexp(inner.ident)})"
                self.send_frame(f, reply)
                self.log("rep", reply)
        finally:
            self.active_debug = None

    def handle_connection(self, conn):
        f = conn.makefile("rwb")
        try:
            while True:
                header = f.read(6)
                if not header or len(header) < 6:
                    break
                length = int(header.decode(), 16)
                data = f.read(length).decode()
                self.log("req", data)
                form, _ = read_sexp(data)
                resp = self.dispatch(form)
                for event in resp.events:
                    self.send_frame(f, event)
                    self.log("evt", event)
                if resp.debug_condition:
                    self.debugger_dialog(f, resp)
                    if resp.close:
                        break
                    continue
                reply = f"(:return (:ok {write_sexp(resp.return_value)}) {write_sexp(resp.ident)})"
                self.send_frame(f, reply)
                self.log("rep", reply)
                if resp.close:
                    break
        finally:
            f.close()

    def serve(self):
        lst = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        lst.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        lst.bind(("127.0.0.1", self.port))
        lst.listen(4)
        print(f"; lrepl swank server: M-x slime-connect RET localhost RET {self.port}", flush=True)
        while True:
            conn, _ = lst.accept()
            try:
                self.handle_connection(conn)
            except (ConnectionError, BrokenPipeError, OSError):
                pass
            finally:
                conn.close()


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4005
    SwankServer(port).serve()


if __name__ == "__main__":
    main()
