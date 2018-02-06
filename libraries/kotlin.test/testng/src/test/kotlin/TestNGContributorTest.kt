package kotlin.test.testng.tests

import org.testng.annotations.*
import org.testng.*
import java.util.concurrent.*

class TestNGContributorTest {

    @Test
    fun smokeTest() {
        Assert.assertEquals("TestNGAsserter", kotlin.test.asserter.javaClass.simpleName)
    }

    @Test
    fun `should fail to contribute if it was run outside of testng`() {
        val q = ArrayBlockingQueue<Any>(1)

        Thread {
            q.put(kotlin.test.asserter)
        }.start()

        Assert.assertEquals("DefaultAsserter", q.take().javaClass.simpleName)
    }

}
