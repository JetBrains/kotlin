// ERROR: Unresolved reference: x
class C {
    private val string = getString()

    default object {

        fun getString(): String? {
            return x()
        }
    }
}