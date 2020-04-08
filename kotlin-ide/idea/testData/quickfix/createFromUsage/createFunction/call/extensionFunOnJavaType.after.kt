// "Create extension function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int? {
    return A().foo(1, "2")
}

private fun A.foo(i: Int, s: String): Int? {
    TODO("Not yet implemented")
}
