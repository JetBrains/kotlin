class Library {
    class object {
        fun call() {
        }

        fun getString(): String {
            return ""
        }
    }
}

class User {
    fun main() {
        Library.call()
        Library.getString().isEmpty()
    }
}