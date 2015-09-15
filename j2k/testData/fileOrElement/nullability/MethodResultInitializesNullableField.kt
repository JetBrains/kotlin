// ERROR: Unresolved reference: x
internal class C {
    private val string = getString()

    companion object {

        internal fun getString(): String? {
            return x()
        }
    }
}