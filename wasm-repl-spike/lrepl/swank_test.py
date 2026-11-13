#!/usr/bin/env python3
"""Fake SLIME client: exercises the lrepl swank server the way slime would.

Starts `python3 swank.py 4010` itself, then runs a scripted session:
handshake -> connection-info -> ping -> define -> evaluate -> re-define
(verify the live slot swap) -> completions -> quit.
"""

import re
import socket
import subprocess
import sys
import time

PORT = 4010


def frame(s):
    payload = s.encode()
    return f"{len(payload):06x}".encode() + payload


class Client:
    def __init__(self, port):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=120)
        self.f = self.sock.makefile("rwb")

    def rex(self, code, ident, as_string=True):
        """as_string=True sends the form quoted (string); False sends it as a
        nested list, which is what real SLIME does."""
        if as_string:
            esc = code.replace('\\', '\\\\').replace('"', '\\"')
            payload = f'(:emacs-rex "{esc}" "lrepl" t {ident} (:protocol-version "2.30"))'
        else:
            inner = code.replace('(', ' (').replace(')', ') ')  # spaced for raw embedding
            payload = f'(:emacs-rex {inner} "lrepl" t {ident} (:protocol-version "2.30"))'
        self.f.write(frame(payload))
        self.f.flush()
        frames = []
        while True:
            hdr = self.f.read(6)
            if not hdr:
                raise EOFError("server closed")
            data = self.f.read(int(hdr.decode(), 16)).decode()
            frames.append(data)
            if re.match(r'\(:return ', data):
                return frames

    def send(self, code, ident, as_string=True):
        """Send a rex without waiting for its reply (the debugger flow holds
        replies until the popup is quit)."""
        if as_string:
            esc = code.replace('\\', '\\\\').replace('"', '\\"')
            payload = f'(:emacs-rex "{esc}" "lrepl" t {ident} (:protocol-version "2.30"))'
        else:
            inner = code.replace('(', ' (').replace(')', ') ')
            payload = f'(:emacs-rex {inner} "lrepl" t {ident} (:protocol-version "2.30"))'
        self.f.write(frame(payload))
        self.f.flush()

    def read_frames(self, until):
        """Read frames until predicate(frame_text, frames_so_far) holds."""
        frames = []
        while True:
            hdr = self.f.read(6)
            if not hdr:
                raise EOFError("server closed")
            frames.append(self.f.read(int(hdr.decode(), 16)).decode())
            if until(frames[-1], frames):
                return frames

    def close(self):
        self.f.close()
        self.sock.close()


def main():
    proc = subprocess.Popen([sys.executable, "swank.py", str(PORT)])
    try:
        time.sleep(1.5)  # let the server bind (image/daemon start lazily)
        c = Client(PORT)

        # real-slime style: nested-list forms + protocol version in extras
        frames = c.rex('(swank:connection-info)', 1, as_string=False)
        print("connection-info [list form] ->", frames[-1][:150], "...")

        # replay slime's actual negotiation sequence:
        frames = c.rex("(swank:swank-require '(swank-sprof swank-mrepl swank-fuzzy))", 2, as_string=False)
        print("swank-require ->", frames[-1])
        assert "swank-mrepl" in frames[-1]

        frames = c.rex('(swank-repl:create-repl nil)', 3, as_string=False)
        print("create-repl ->", frames[-1])
        assert '"lrepl"' in frames[-1] and '"lrepl> "' in frames[-1]

        frames = c.rex('(swank:ping t 0)', 4, as_string=False)
        print("ping ->", frames[-1])

        frames = c.rex('(swank:interactive-eval "40 + 2")', 5, as_string=False)
        print("interactive-eval 40+2 ->", frames[-1])

        # swank-repl namespace + :window-width kwarg, as slime sends it
        frames = c.rex('(swank-repl:listener-eval "fun scale(n: Int): Int = n * 10" :window-width 80)', 6)
        print("swank-repl:listener-eval define ->", frames[-1])

        frames = c.rex('(swank-repl:listener-eval "scale(6)" :window-width 80)', 7)
        print("swank-repl:listener-eval scale(6) ->", frames[-1])
        for f in frames[:-1]:
            print("   event:", f)

        frames = c.rex('(swank:interactive-eval "fun scale(n: Int): Int = n * 100")', 8)
        print("redefine scale ->", frames[-1])

        frames = c.rex('(swank-repl:listener-eval "scale(6)" :window-width 80)', 9)
        print("listener-eval scale(6) after redefine ->", frames[-1])
        for f in frames[:-1]:
            print("   event:", f)

        # C-c C-c: compile-string-for-emacs with slime's exact argument shape
        frames = c.rex('(swank:compile-string-for-emacs "fun quad(n: Int): Int = scale(n) * 4" "test.kt" \'((:position 42) (:line 3 1)) "/tmp/test.kt" \'())', 10)
        print("compile-string success ->", frames[-1])

        frames = c.rex('(swank:compile-string-for-emacs "fun broken(n: Int): Int = undefined_thing(n)" "test.kt" \'((:position 42) (:line 3 1)) "/tmp/test.kt" \'())', 11)
        print("compile-string failure ->", frames[-1])
        assert ':location' in frames[-1] and ':buffer' in frames[-1]

        frames = c.rex('(swank:simple-completions "sc" "lrepl")', 12, as_string=False)
        print("simple-completions sc ->", frames[-1])

        frames = c.rex('(swank:completions "sc" "lrepl")', 13)
        print("completions sc ->", frames[-1])

        # ---- SLDB: a runtime trap opens the debugger popup. The trapping
        # rex gets NO :return until the debugger quits; then it is answered
        # (:abort <condition>), like swank.lisp's sldb-loop.
        frames = c.rex('(swank-repl:listener-eval "fun trapbang(): Int = (null as String).length" :window-width 80)', 15)
        c.send('(swank-repl:listener-eval "trapbang()" :window-width 80)', 16)
        ev = c.read_frames(lambda d, fs: d.startswith("(:debug-activate 1 1"))
        trap_events = [fr for fr in ev if fr.startswith("(:debug")]
        assert any(fr.startswith("(:debug 1 1") for fr in trap_events), ev
        assert not any(fr.startswith("(:return") for fr in ev), "trap rex answered too early"
        print("runtime trap -> :debug + :debug-activate popup events")

        frames = c.rex('(swank:debugger-info-for-emacs 0 10)', 17, as_string=False)
        info = frames[-1]
        assert ':restartable' in info and 'ABORT' in info and 'Kotlin/Wasm runtime trap' in info
        print("debugger-info-for-emacs ->", info[:110], "...")

        # after throw-to-toplevel is acked, the popup closes (:debug-return)
        # and the original rex 16 is answered (:abort <condition>)
        c.send('(swank:throw-to-toplevel)', 18, as_string=False)
        tail = c.read_frames(lambda d, fs: d.startswith("(:return (:abort") and d.rstrip().endswith("16)"))
        assert any(fr.startswith("(:return (:ok") and " 18)" in fr for fr in tail), tail
        assert any(fr.startswith("(:debug-return 1 1") for fr in tail), tail
        print("throw-to-toplevel -> popup closed, rex 16 aborted:",
              [fr for fr in tail if "(:abort" in fr][0][:80])

        # after the debugger: the image still works
        frames = c.rex('(swank:interactive-eval "scale(3)")', 19, as_string=False)
        print("interactive-eval after sldb ->", frames[-1])
        assert "300" in frames[-1]

        # ---- C-c C-c that Just Works: point inside a class member — the
        # server re-submits the ENCLOSING class from the file, whatever
        # fragment slime grabbed as the "defun".
        src = ('import kotlin.math.abs\n'
               '\n'
               'class CellBox(var n: Int) {\n'
               '    fun bump(): Int {\n'
               '        n = n + 1\n'
               '        return n\n'
               '    }\n'
               '}\n')
        fname = "/tmp/lrepl_ccc_test.kt"
        with open(fname, "w") as fh:
            fh.write(src)
        point = src.index("return n")
        junk_region = "n = n + 1"  # a member fragment, not a valid program
        frames = c.rex(f'(swank:compile-string-for-emacs "{junk_region}" "test.kt" '
                       f"'((:position {point}) (:line 6 1)) \"{fname}\" '())", 20)
        assert ':compilation-result' in frames[-1] and 'nil t' in frames[-1].replace('()', 'nil'), frames[-1]
        print("C-c C-c inside member -> enclosing class compiled:", frames[-1][:90])

        frames = c.rex('(swank:interactive-eval "CellBox(1).bump()")', 21, as_string=False)
        print("CellBox works ->", frames[-1])
        assert "=> 2" in frames[-1]

        # ---- describe-symbol (C-c C-d d)
        frames = c.rex('(swank:describe-symbol "CellBox")', 22, as_string=False)
        print("describe-symbol CellBox ->", frames[-1][:100], "...")
        assert "class CellBox" in frames[-1]

        # ---- find-definitions (M-.)
        frames = c.rex('(swank:find-definitions-for-emacs "CellBox")', 23, as_string=False)
        print("find-definitions CellBox ->", frames[-1][:150], "...")
        assert ":file" in frames[-1] and ":position" in frames[-1]

        frames = c.rex('(swank:quit-lisp)', 14, as_string=False)
        print("quit ->", frames[-1])
        c.close()
        print("SWANK TEST PASSED")
    finally:
        proc.terminate()
        proc.wait(timeout=10)


if __name__ == "__main__":
    main()
