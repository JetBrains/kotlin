internal object Library {
    fun call() {
    }

    val string: String
        get() {
            return ""
        }
}

internal class User {
    fun main() {
        Library.call()
        Library.string.isEmpty()
    }
}