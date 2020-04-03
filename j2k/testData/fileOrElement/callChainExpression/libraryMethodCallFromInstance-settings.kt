// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
internal class Library {

    val string: String
        get() = ""

    fun call() {}
}

internal class User {
    fun main() {
        val lib: Library = Library()
        lib.call()
        lib.string.isEmpty()

        Library().call()
        Library().string.isEmpty()
    }
}