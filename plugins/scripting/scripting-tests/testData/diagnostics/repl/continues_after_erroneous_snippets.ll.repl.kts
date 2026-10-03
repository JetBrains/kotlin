// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

<!SYNTAX!>)<!>(<!SYNTAX!><!>

// SNIPPET

fun foo() = 98

// SNIPPET

foo(1)

// SNIPPET

val res = <!UNRESOLVED_REFERENCE!>foo<!>()

// SNIPPET

// the declarations of a snippet failing in the checkers are not visible to the later snippets
<!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val a: String<!>
fun bar() = 1

// SNIPPET

val res2 = <!UNRESOLVED_REFERENCE!>bar<!>() + <!UNRESOLVED_REFERENCE!>a<!>.length
