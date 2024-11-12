
// SNIPPET

println("${<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>C().x<!>}")
println("$<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x<!>")

class C(val x: String = "Hi")
val x = "Hi"

println("$x")
println("${C().x}")

