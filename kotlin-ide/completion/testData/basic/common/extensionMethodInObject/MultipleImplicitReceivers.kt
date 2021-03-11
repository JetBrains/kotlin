class T

object O {
    fun A.fooForA() {}
    fun A.B.fooForB() {}
    fun A.B.C.fooForC() {}
    fun T.fooForT() {}
}

class A {
    inner class B {
        fun T.usage() {
            foo<caret>
        }

        inner class C {}
    }
}

// EXIST: { lookupString: "fooForA", itemText: "fooForA" }
// EXIST: { lookupString: "fooForB", itemText: "fooForB" }
// EXIST: { lookupString: "fooForT", itemText: "fooForT" }
// ABSENT: fooForC