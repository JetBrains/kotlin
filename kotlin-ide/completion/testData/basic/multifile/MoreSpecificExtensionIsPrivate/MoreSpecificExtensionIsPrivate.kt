package ppp

import dependency.*

class C

fun foo(c: C) {
    c.xx<caret>
}

// we should not include inaccessible extension even on the second completion because the call won't resolve into it anyway
// INVOCATION_COUNT: 2
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "() for Any in dependency", typeText: "Int" }
// NOTHING_ELSE
