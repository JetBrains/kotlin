// SNIPPET

<!SYNTAX!>)<!>(<!SYNTAX!><!>

// SNIPPET

fun foo() = 98

// SNIPPET

foo(<!TOO_MANY_ARGUMENTS!>1<!>)

// SNIPPET

val res = foo()

// SNIPPET

// the declarations of a snippet failing in the checkers are not visible to the later snippets
<!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val a: String<!>
fun bar() = 1

// SNIPPET

val res2 = <!UNRESOLVED_REFERENCE!>bar<!>() + <!UNRESOLVED_REFERENCE!>a<!>.length
