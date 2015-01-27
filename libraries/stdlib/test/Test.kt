package kotlinhack.test

import junit.framework.*

import kotlin.io.*
import kotlin.util.*
import java.util.*

import org.junit.*
import org.junit.runner.*
import org.junit.runner.notification.*
import org.junit.Assert

class TestBuilt<T>(name: String, val builder: TestBuilder<T>, val test: TestBuilt<T>.() -> Unit) : TestCase(name) {
    private var myState: T? = null

    var state : T
        get() = myState!!
        set(newState: T) { myState = newState }

    override fun countTestCases(): Int = 1

    override fun setUp() = this.(builder.setUp)()

    override fun tearDown() = this.(builder.tearDown)()

    override fun runTest() = this.(test)()
}

open class TestBuilder<T>(name: String) {
    val mySuite : TestSuite = TestSuite(name)

    public var setUp : TestBuilt<T>.() -> Unit = {}

    public var tearDown : TestBuilt<T>.() -> Unit = {}

    fun String.minus(test: TestBuilt<T>.() -> Unit) {
        mySuite.addTest(TestBuilt<T>(this, this@TestBuilder, test))
    }
}

private val currentTestBuilder = ThreadLocal<TestSuite> ()

private fun <T> testSuite(builder: TestBuilder<T>, description: TestBuilder<T>.() -> Unit) : TestSuite? {
    val currentTestSuite = currentTestBuilder.get()
    currentTestBuilder.set(builder.mySuite)
    try {
        builder.(description)()
        return if(currentTestSuite != null) {
            currentTestSuite.addTest(builder.mySuite)
            null
        }
        else {
            builder.mySuite
        }
    }
    finally {
        currentTestBuilder.set(currentTestSuite)
    }
}

fun <T> testSuite(name: String,  description: TestBuilder<T>.() -> Unit) : TestSuite? =
    testSuite(TestBuilder<T>(name), description)

fun assertNot(message: String, block: ()-> Boolean) {
    Assert.assertTrue(message, block())
}

fun assertNot(block: ()-> Boolean) = assertNot(block.toString(), block)

fun assertTrue(actual: Boolean, message: String = "") {
  return assertEquals(true, actual, message)
}

fun assertFalse(actual: Boolean, message: String = "") {
    return assertEquals(false, actual, message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
  Assert.assertEquals(message, expected, actual)
}

fun assertNotNull(actual: Any?, message: String = "") {
  Assert.assertNotNull(message, actual)
}

fun assertNull(actual: Any?, message: String = "") {
  Assert.assertNull(message, actual)
}

fun fail(message: String = "") {
  Assert.fail(message)
}

fun <T> expect(expected: T, block: ()-> T) {
  expect(expected, block.toString(), block)
}

fun <T> expect(expected: T, message: String, block: ()-> T) {
  val actual = block()
  assertEquals(expected, actual, message)
}

fun fails(block: ()-> Any) {
  try {
    block()
    Assert.fail("Expected an exception to be thrown")
  } catch (e: Exception) {
    println("Caught excepted exception: $e")
    // OK
  }
}

fun <T: Exception> failsWith(block: ()-> Any) {
  try {
    block()
    Assert.fail("Expected an exception to be thrown")
  } catch (e: T) {
    println("Caught excepted exception: $e")
    // OK
  }
}

fun todo(block: ()-> Any) {
  println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
}

/*
TODO we could maybe create our own test runner for JUnit
to avoid a runtime dependency on JUnit for running tests?

class KotlinTestRunner() : Runner() {

  override fun getDescription(): Description? {
    return null
  }

  override fun run(notifier: RunNotifier?) {
    println("About to run a test case on ${this}")
  }
}
*/

// TODO no annotations yet?
//@RunWith(KotlinTestRunner)
abstract class TestSupport() : TestCase() {
}
