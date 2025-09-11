// The test should be moved/promoted to codegen one after fixing KT-74350
// SNIPPET

var o = "o"

// SNIPPET

var k = "k"

class C {
    fun foo(): String = o + k
    fun refO() = ::o // References to variables aren't supported yet - KT-74350
    fun refK() = ::k // same as above
}

val res1 = C().foo()

// EXPECTED: res1 == "ok"

// SNIPPET

val c = C()

c.refO().set("O")
c.refK().set("K")

val res2 = c.foo()

// EXPECTED: res2 == "OK"

val oo = c.refO().get()
val kk = c.refK().get()

val res3 = oo + kk

// EXPECTED: res3 == "OK"
