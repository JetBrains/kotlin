// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

val foo = "foo"
val bar = 42
fun baz(i: Int) = i + 1

// SNIPPET

class C(val s: String) {
    fun f(): String = s
    fun g(n: Int) = s.length + n
}

// SNIPPET

fun <!UNRESOLVED_REFERENCE!>C<!>.sfoo() = <!UNRESOLVED_REFERENCE!>s<!> + <!UNRESOLVED_REFERENCE!>foo<!>
fun <!UNRESOLVED_REFERENCE!>C<!>.sbar() = <!UNRESOLVED_REFERENCE!>g<!>(<!UNRESOLVED_REFERENCE!>bar<!>)

// SNIPPET

val res1 = <!UNRESOLVED_REFERENCE!>C<!>(<!UNRESOLVED_REFERENCE!>foo<!>).sfoo()
val res2 = <!UNRESOLVED_REFERENCE!>C<!>(<!UNRESOLVED_REFERENCE!>foo<!>).sbar()
val res3 = <!UNRESOLVED_REFERENCE!>baz<!>(<!UNRESOLVED_REFERENCE!>bar<!>)

