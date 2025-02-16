// The test should be moved/promoted to codegen one after fixing KT-74350
// SNIPPET

var o = "o"

// SNIPPET

var k = "k"

class C {
    fun foo(): String = o + k
    fun refO() = ::<!UNSUPPORTED!>o<!> // References to variables aren't supported yet - KT-74350
    fun refK() = ::<!UNSUPPORTED!>k<!> // same as above
}

val res1 = C().foo()

// EXPECTED: res1 == "ok"

// SNIPPET

val c = C()

c.refO().set(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>"O"<!>)<!>
c.refK().set(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>"K"<!>)<!>

val res2 = c.foo()

// EXPECTED: res2 == "OK"

val oo = c.refO().get<!NO_VALUE_FOR_PARAMETER!>()<!>
val kk = c.refK().get<!NO_VALUE_FOR_PARAMETER!>()<!>

val res3 = oo <!UNRESOLVED_REFERENCE!>+<!> kk

// EXPECTED: res3 == "OK"
