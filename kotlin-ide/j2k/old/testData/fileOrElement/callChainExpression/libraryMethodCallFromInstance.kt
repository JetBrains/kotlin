internal class Library {

    val string: String
        get() = ""

    fun call() {}
}

internal class User {
    fun main() {
        val lib = Library()
        lib.call()
        lib.string.isEmpty()

        Library().call()
        Library().string.isEmpty()
    }
}