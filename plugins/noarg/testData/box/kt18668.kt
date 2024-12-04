// WITH_STDLIB

annotation class NoArg

@NoArg
class AuthenticationConfiguration(tokenExpiresIn: Long) {
    var tokenExpiryDate: String?

    init {
        tokenExpiryDate = tokenExpiresIn.toString()
    }
}

fun box(): String {
    val instance = AuthenticationConfiguration::class.java.newInstance()

    if (instance.tokenExpiryDate != null) {
        return "Initializer invoked"
    }

    return "OK"
}