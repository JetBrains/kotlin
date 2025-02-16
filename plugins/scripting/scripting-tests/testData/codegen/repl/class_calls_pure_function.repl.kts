
// SNIPPET

fun f() = "OK"

// SNIPPET

class C {
    val v1: String = f()
    val v2: String
        get() = f()
}

// SNIPPET

val res = "${C().v1}_${C().v2}"

// EXPECTED: res == "OK_OK"
