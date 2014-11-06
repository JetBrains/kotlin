// ERROR: Property must be initialized or be abstract
class Library {
    public val myString: String
}

class User {
    fun main() {
        Library().myString.isEmpty()
    }
}