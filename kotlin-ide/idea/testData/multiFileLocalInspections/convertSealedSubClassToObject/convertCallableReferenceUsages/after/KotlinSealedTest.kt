import seal.*

class KotlinSealedTest {
    fun testSeal() {
        val internalFunction = SubSealed::internalFunction

        val nestedClass = SubSealed.Nested()

        SubSealed.Nested()
        SubSealed::Nested

        SubSealed.internalFunction()
        SubSealed::internalFunction
        SubSealed::internalFunction
    }
}