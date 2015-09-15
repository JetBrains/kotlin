internal class Library {
    internal fun call() {
    }

    internal fun getString(): String {
        return ""
    }
}

internal class User {
    internal fun main() {
        val lib = Library()
        lib.call()
        lib.getString().isEmpty()

        Library().call()
        Library().getString().isEmpty()
    }
}