// ERROR: Unresolved reference: x
class C {
    private val string = getString()

    companion object {

        fun getString(): String? {
            return x()
        }
    }
}