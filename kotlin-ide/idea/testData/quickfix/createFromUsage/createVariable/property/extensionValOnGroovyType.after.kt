// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

private val A.foo: String?
    get() {
        TODO("Not yet implemented")
    }

fun test(): String? {
    return A().foo
}