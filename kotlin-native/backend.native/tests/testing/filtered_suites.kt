package kotlin.test.tests

import kotlin.test.*

private fun hook(message: String) {
    print("Hook: ")
    println(message)
}

class A {
    @Test
    fun foo() {}

    @Test
    fun common() {}

    @Ignore
    @Test
    fun ignored() {}

    companion object {
        @BeforeClass
        fun before() = hook("A.before")

        @AfterClass
        fun after() = hook("A.after")
    }
}

@Ignore
class Ignored {
    @Test
    fun bar() {}

    @Test
    fun common() {}

    companion object {
        @BeforeClass
        fun before() = hook("Ignored.before")

        @AfterClass
        fun after() = hook("Ignored.after")
    }
}

@BeforeClass
fun before() = hook("Filtered_suitesKt.before")

@AfterClass
fun after() = hook("Filtered_suitesKt.after")

@Test
fun baz() {}

@Test
fun common() {}
