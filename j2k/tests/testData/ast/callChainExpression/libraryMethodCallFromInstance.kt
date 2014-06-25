class Library {
    fun call() {
    }

    fun getString(): String {
        return ""
    }
}

class User {
    fun main() {
        val lib = Library()
        lib.call()
        lib.getString().isEmpty()

        Library().call()
        Library().getString().isEmpty()
    }
}