// "Create member function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().foo<String, Int>(1, "2")
}