internal class Library {
    fun call() {}

    val string: String
        get() = ""

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