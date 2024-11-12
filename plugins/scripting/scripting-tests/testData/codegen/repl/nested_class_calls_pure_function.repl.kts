
// SNIPPET

fun f() = "OK"

// SNIPPET

class Outer {
    class C {
        val v1: String = f()
        val v2: String
            get() = f()
    }
}

// SNIPPET

val res = "${Outer.C().v1}" +
        "_${Outer.C().v2}"

// EXPECTED: res == "OK_OK"
