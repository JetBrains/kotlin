
// SNIPPET

println("${<!UNRESOLVED_REFERENCE!>C<!>().x}")

// SNIPPET

println("$<!UNRESOLVED_REFERENCE!>x<!>")

// SNIPPET

class C(val x: String = "Hi")

// SNIPPET

val x = "Hi"

// SNIPPET

println("$x")

// SNIPPET

println("${C().x}")
