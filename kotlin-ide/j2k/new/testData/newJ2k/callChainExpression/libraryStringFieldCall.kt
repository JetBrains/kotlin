internal class Library {
    val myString: String? = null
}

internal class User {
    fun main() {
        Library().myString!!.isEmpty()
    }
}