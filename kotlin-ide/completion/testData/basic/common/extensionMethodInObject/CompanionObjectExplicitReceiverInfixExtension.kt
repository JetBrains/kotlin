class T

class A {
    companion object {
        infix fun T.fooExtension(i: Int) {}
    }
}

fun usage(t: T) {
    t foo<caret>
}

// EXIST: { lookupString: "fooExtension", itemText: "fooExtension" }
