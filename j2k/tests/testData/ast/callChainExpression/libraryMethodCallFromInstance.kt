open class Library() {
    open fun call() {
    }

    open fun getString(): String? {
        return ""
    }
}
open class User() {
    open fun main() {
        var lib: Library? = Library()
        lib?.call()
        lib?.getString()?.isEmpty()

        Library().call()
        Library().getString()?.isEmpty()
    }
}