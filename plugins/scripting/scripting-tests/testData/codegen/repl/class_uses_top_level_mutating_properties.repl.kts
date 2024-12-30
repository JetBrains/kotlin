
// SNIPPET

var o = "o"

// SNIPPET

var k = "k"

class C {
    fun foo(): String = o + k
}

val res1 = C().foo()

// EXPECTED: res1 == "ok"

// SNIPPET

o = "O"
k = "K"

val res2 = C().foo()

// EXPECTED: res2 == "OK"
