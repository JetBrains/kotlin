// ERROR: Unresolved reference: x
internal class C {
    private val s: String? = x()

    fun foo() {
        if (s == null) {
            print("null")
        }
    }
}