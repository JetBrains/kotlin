// ERROR: Property must be initialized or be abstract
internal class Library {
    val myString: String
}

internal class User {
    fun main() {
        Library().myString.isEmpty()
    }
}