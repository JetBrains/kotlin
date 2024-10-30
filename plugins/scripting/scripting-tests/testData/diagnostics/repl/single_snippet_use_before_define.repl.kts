
// SNIPPET

println("${<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().x<!>}")
println("$<!UNRESOLVED_REFERENCE!>x<!>")

class C(val x: String = "Hi")
val x = "Hi"

println("$x")
println("${C().x}")

