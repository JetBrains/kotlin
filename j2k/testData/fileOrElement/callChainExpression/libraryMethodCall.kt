internal object Library {
    fun call() {
    }

    fun getString(): String {
        return ""
    }
}

internal class User {
    fun main() {
        Library.call()
        Library.getString().isEmpty()
    }
}