package p

class C

operator fun C.get(p: Int): Int = 0

fun foo(c: C) {
    c.<caret>
}

// EXIST: { lookupString: "[]", itemText: "[]", tailText: "(p: Int) for C in p", typeText: "Int", attributes: "bold" }
