#!/usr/bin/env python3
"""Gate: the IMAGE registry is the compiler's only previous-state store.

Two scenarios, both with the IC cache wiped at the critical moment (the
cache serves compilation speed only — the compiler has no IC history
path):

1. FIRST bump after the wipe: define a class, create an instance, WIPE
   THE CACHE, redefine the class with an added field. The previous v1
   shape can only come from the previous generation's hot-reload-registry
   custom section (the COMPLETE payload table — an unbumped class's
   current entry). The old instance must migrate lazily on first
   new-code field access: surviving field preserved, new field defaulted.
2. SECOND bump after another wipe: redefine further and migrate again —
   the registry accumulates and re-ferries across generations.

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
