internal object Library {
    fun call() {}

    val string: String
        get() = ""

}

internal class User {
    fun main() {
        Library.call()
        Library.string.isEmpty()
    }
}