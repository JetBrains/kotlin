class T

object TopLevel {
    object Nested {
        fun T.foo() {}
    }
}

fun T.usage() {
    f<caret>
}

// ELEMENT: foo