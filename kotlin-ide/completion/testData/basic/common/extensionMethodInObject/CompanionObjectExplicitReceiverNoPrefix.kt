class T

class A {
    companion object {
        fun T.fooExtension() {}
        val T.fooProperty get() = 10
    }
}

fun usage(t: T) {
    t.<caret>
}

// EXIST: { lookupString: "fooExtension", itemText: "fooExtension" }
// EXIST: { lookupString: "fooProperty", itemText: "fooProperty" }