internal object Library {
    internal fun call() {
    }

    internal fun getString(): String {
        return ""
    }
}

internal class User {
    internal fun main() {
        Library.call()
        Library.getString().isEmpty()
    }
}