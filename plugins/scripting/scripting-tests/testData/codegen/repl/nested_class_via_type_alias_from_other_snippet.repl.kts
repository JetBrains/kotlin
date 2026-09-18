
// SNIPPET

class Outer {
    class Nested {
        fun f() = "OK"
    }
}

// SNIPPET

typealias N = Outer.Nested

// SNIPPET

val res = N().f()

// EXPECTED: res == "OK"
