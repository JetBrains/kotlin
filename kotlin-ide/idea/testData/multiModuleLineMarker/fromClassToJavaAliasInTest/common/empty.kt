// !CHECK_HIGHLIGHTING

data class User(val name: String, val password: String)

expect open class Expected {
    val user: User
}