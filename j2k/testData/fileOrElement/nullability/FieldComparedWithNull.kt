// ERROR: Unresolved reference: x
class C {
    private val s = x()

    fun foo() {
        if (s == null) {
            print("null")
        }
    }
}