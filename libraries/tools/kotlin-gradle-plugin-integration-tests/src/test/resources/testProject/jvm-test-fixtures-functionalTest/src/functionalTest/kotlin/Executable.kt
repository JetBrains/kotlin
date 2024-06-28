import kotlin.test.Test

class Executable {
    @Test
    fun runTest() {
        MainJava()
        MainKotlin()
        MainKotlinInternal()
        println("src/main OK!")

        TestJava()
        TestKotlin()
        TestKotlinInternal()
        println("src/test OK!")

        TestFixturesJava()
        TestFixturesKotlin()
        TestFixturesKotlinInternal()
        println("src/testFixtures OK!")

        FunctionalTestKotlin()
        FunctionalTestJava()
        println("src/functionalTest OK!")
    }
}