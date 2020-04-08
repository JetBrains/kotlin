package p

open class A

interface I {
    interface Nested
}

class B : A(), I.Nested {
    fun foo() {
        super<I<caret>
    }
}

// EXIST: { lookupString: "Nested", allLookupStrings: "I, Nested", itemText: "I.Nested", tailText: " (p)" }
// NOTHING_ELSE