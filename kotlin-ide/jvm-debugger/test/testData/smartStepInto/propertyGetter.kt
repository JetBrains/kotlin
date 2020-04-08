fun foo() {
    a + b + c + d<caret>
}

val a = 1

val b = 1
    get

val c: Int
    get() = 1

val d: Int
    get() {
        return 1
    }

// EXISTS: getter for c: Int, getter for d: Int