package kotlin.test.junit.tests

import org.junit.Assert
import kotlin.test.*
import java.util.concurrent.*
import kotlin.test.junit.JUnitAsserter

class JUnitContributorTest {
    @Test
    fun smokeTest() {
        assertSame(JUnitAsserter, kotlin.test.asserter)
        Assert.assertEquals(JUnitAsserter::class.java.simpleName, kotlin.test.asserter.javaClass.simpleName)
    }

    @Test
    fun parallelThreadGetsTheSameAsserter() {
        val q = ArrayBlockingQueue<Any>(1)

        Thread {
            q.put(asserter)
        }.start()

        assertSame(kotlin.test.asserter, q.take())
    }

}
