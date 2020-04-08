// "Make private and implements 'setName'" "true"
// DISABLE-ERRORS
class A(<caret>var name: String) : JavaInterface {
    override fun getName(): String {
        TODO("Not yet implemented")
    }
}