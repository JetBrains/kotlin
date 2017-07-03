package test

annotation class NoArg

@NoArg
class AuthenticationConfiguration(tokenExpiresIn: Long) {
    var tokenExpiryDate: String

    init {
        tokenExpiryDate = tokenExpiresIn.toString()
    }
}