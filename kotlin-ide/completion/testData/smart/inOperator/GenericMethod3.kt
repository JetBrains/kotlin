interface X<T>

operator fun<T> X<T>.contains(t: T): Boolean = true

interface A {
    fun<T> createX(t: T): X<T>

    fun foo(s: String) {
        if (s in <caret>)
    }
}

// EXIST: { lookupString:"createX", itemText: "createX", tailText: "(t: String)", typeText:"X<String>" }
