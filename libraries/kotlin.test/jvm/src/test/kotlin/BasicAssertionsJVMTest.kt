package kotlin.test.tests

import kotlin.test.*

class BasicAssertionsJVMTest {

    @Test
    fun testFailsWithClassMessage() {
        @Suppress("UNCHECKED_CAST")
        (assertFailsWith((Class.forName("java.lang.IllegalArgumentException") as Class<Throwable>).kotlin) {
            throw IllegalArgumentException()
        })
    }
}
