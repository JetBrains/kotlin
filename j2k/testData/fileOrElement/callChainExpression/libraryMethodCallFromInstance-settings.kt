// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
internal class Library {
    fun call() {}

    val string: String?
        get() = ""
}

internal class User {
    fun main() {
        val lib: Library = Library()
        lib.call()
        lib.string!!.isEmpty()

        Library().call()
        Library().string!!.isEmpty()
    }
}