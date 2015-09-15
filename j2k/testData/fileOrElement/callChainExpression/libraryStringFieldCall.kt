// ERROR: Property must be initialized or be abstract
internal class Library {
    val myString: String
}

internal class User {
    internal fun main() {
        Library().myString.isEmpty()
    }
}