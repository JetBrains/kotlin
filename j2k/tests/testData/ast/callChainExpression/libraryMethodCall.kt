open class Library() {
    class object {
        open fun call() {
        }
        open fun getString(): String? {
            return ""
        }
    }
}
open class User() {
    open fun main() {
        Library.call()
        Library.getString()?.isEmpty()
    }
}