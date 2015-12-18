package kotlinx.testing.tests

import org.junit.*
import java.util.concurrent.*

class JUnitContributorTest {
    @Test
    fun smokeTest() {
        Assert.assertEquals("JUnitAsserter", asserter.`class`.simpleName)
    }

    @Test
    fun `should fail to contribute if it was run outside of junit`() {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val q = ArrayBlockingQueue<java.lang.Object>(1)

        Thread {
            q.put(asserter)
        }.start()

        Assert.assertEquals("DefaultAsserter", q.take().`class`.simpleName)
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val asserter: java.lang.Object
        get() = Class.forName("kotlin.test.AssertionsKt").getMethod("getAsserter").invoke(null) as java.lang.Object
}
