// ERROR: Unresolved reference: x
internal class C {
    private val string = getString()

    companion object {

        fun getString(): String? {
            return x()
        }
    }
}