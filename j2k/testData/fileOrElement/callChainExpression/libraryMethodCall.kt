object Library {
    fun call() {
    }

    fun getString(): String {
        return ""
    }
}

class User {
    fun main() {
        Library.call()
        Library.getString().isEmpty()
    }
}