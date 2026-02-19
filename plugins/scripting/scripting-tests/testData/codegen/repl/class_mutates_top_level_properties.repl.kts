
// SNIPPET

var o = "o"

// SNIPPET

var k = "k"

class C {
    fun foo(): String = o + k
    fun changeO(v: String) { o = v }
    fun changeK(v: String) { k = v }
}

val res1 = C().foo()

// EXPECTED: res1 == "ok"

// SNIPPET

val c = C()

c.changeO("O")
c.changeK("K")

val res2 = c.foo()

// EXPECTED: res2 == "OK"
