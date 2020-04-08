package p

class Outer {
    interface T {
        object Null : T { }
    }
}

fun foo(): Outer.T {
    return <caret>
}

// ELEMENT: Null
