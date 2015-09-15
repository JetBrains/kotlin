// ERROR: Unresolved reference: x
internal class C {
    private val s = x()

    internal fun foo() {
        if (s == null) {
            print("null")
        }
    }
}