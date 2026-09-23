
// SNIPPET

class Outer {
    class Nested {
        val v: String = "O"
        fun f() = "K"

        class Deep {
            fun g() = "!"
        }
    }
}

// SNIPPET

val nested = Outer.Nested()
val res = nested.v + nested.f() + Outer.Nested.Deep().g()

// EXPECTED: res == "OK!"
