internal object Library {

    val string: String
        get() = ""

    fun call() {}
}

internal class User {
    fun main() {
        Library.call()
        Library.string.isEmpty()
    }
}