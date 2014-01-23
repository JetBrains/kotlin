open class Library() {
    public val myString: String? = null
}

open class User() {
    open fun main() {
        Library.myString?.isEmpty()
    }
}