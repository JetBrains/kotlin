
// SNIPPET

println("${<!UNRESOLVED_REFERENCE!>C<!>().x}")
println("$<!UNRESOLVED_REFERENCE!>x<!>")

class C(val x: String = "Hi")
val x = "Hi"

println("$x")
println("${C().x}")

