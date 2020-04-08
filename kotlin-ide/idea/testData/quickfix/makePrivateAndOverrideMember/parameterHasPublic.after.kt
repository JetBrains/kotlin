// "Make private and implements 'getName'" "true"
// DISABLE-ERRORS
class A(private val name: String) : JavaInterface {
    override fun getName(): String {
        TODO("Not yet implemented")
    }
}