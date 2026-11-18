#!/usr/bin/env python3
"""Gate: the IMAGE resolves migration — a compile has NO previous state.

The layout era (SBCL layouts/wrappers): a class's shape is a content
hash (the wrapper id), instances carry theirs in the payload, and the
image's LAYOUTS table (stdlib, registered at load) holds every
wrapper's slot descriptors + accessor closures. Migration is
interpreted at RUNTIME — update-instance copies same-named
same-typed slots through the layouts' reader/writer funcrefs — so
wiping any cache loses nothing at all. Both scenarios keep the IC
cache wiped at the critical moment to prove there is no hidden
compile-time history:

1. FIRST bump after the wipe: added field — the old instance
   migrates lazily on first new-code field access; the surviving
   field is preserved, the added slot reads its type default.
2. SECOND bump after another wipe: migrate again through the
   image's accumulated layouts.

Run: LREPL_WORK=<scratch> python3 registry_test.py
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lrepl import Lrepl

r = Lrepl()


def h(line):
    ok, out = r.handle(line)
    print(f">> {line[:58]:60s} => {'OK ' if ok else 'ERR'} {str(out)[:60]}")
    return ok, out


h('class Acc(var bal: Int) { fun balance(): Int = bal; fun tag(): String = "acc" }')
h('val acc = Acc(100)')
shutil.rmtree(r.image.ic_cache, ignore_errors=True)
print(">> IC CACHE WIPED — first bump must come from the image registry alone")

h('class Acc(var bal: Int, val owner: String) { fun balance(): Int = bal; fun tag(): String = "acc" }')
h('fun desc(a: Acc): String = "owner=" + a.owner')
ok, out = h('desc(acc)')
assert ok and out == "owner=null", out          # lazy migration fired, owner defaulted
ok, out = h('acc.balance()')
assert ok and out == "100", out                  # surviving field preserved

shutil.rmtree(r.image.ic_cache, ignore_errors=True)
print(">> IC CACHE WIPED AGAIN — second bump, registry must accumulate")

h('class Acc(var bal: Int, val owner: String, val tag2: String) { fun balance(): Int = bal; fun tag(): String = "acc" }')
h('fun desc2(a: Acc): String = "owner=" + a.owner + " tag2=" + a.tag2')
ok, out = h('desc2(acc)')
assert ok and out == "owner=null tag2=null", out  # v2 -> v3 migration through the re-ferried registry
ok, out = h('acc.balance()')
assert ok and out == "100", out
print("IMAGE-REGISTRY-ONLY MIGRATION PASSED (first and second bumps, cache wiped each time)")
