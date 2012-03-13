/**
 * A number of helper methods for writing Kool unit tests
 */
package kotlin.test

import java.util.ServiceLoader

private var _asserter: Asserter? = null

public var asserter: Asserter
    get() {
        if (_asserter == null) {
            val klass = javaClass<Asserter>()
            val loader = ServiceLoader.load(klass)
            for (a in loader) {
                if (a != null) {
                    _asserter = a
                    break
                }
            }
            if (_asserter == null) {
                _asserter = DefaultAsserter()
            }
            //debug("using asserter $_asserter")
        }
        return _asserter.sure()
    }

    set(value) {
        _asserter = value
    }

/** Asserts that the given block returns true */
inline fun assertTrue(message: String, block: ()-> Boolean) {
    val actual = block()
    asserter.assertTrue(message, actual)
}

/** Asserts that the given block returns true */
inline fun assertTrue(block: ()-> Boolean) = assertTrue(block.toString(), block)

/** Asserts that the given block returns false */
inline fun assertNot(message: String, block: ()-> Boolean) {
    assertTrue(message){ !block() }
}

/** Asserts that the given block returns true */
inline fun assertNot(block: ()-> Boolean) = assertNot(block.toString(), block)

/** Asserts that the expression is true with an optional message */
inline fun assertTrue(actual: Boolean, message: String = "") {
    return assertEquals(true, actual, message)
}

/** Asserts that the expression is false with an optional message */
inline fun assertFalse(actual: Boolean, message: String = "") {
    return assertEquals(false, actual, message)
}

/** Asserts that the expected value is equal to the actual value, with an optional message */
inline fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
    asserter.assertEquals(message, expected, actual)
}

/** Asserts that the expression is not null, with an optional message */
inline fun assertNotNull(actual: Any?, message: String = "") {
    asserter.assertNotNull(message, actual)
}

/** Asserts that the expression is null, with an optional message */
inline fun assertNull(actual: Any?, message: String = "") {
    asserter.assertNull(message, actual)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional message */
inline fun fail(message: String = "") {
    asserter.fail(message)
}

/** Asserts that given function block returns the given expected value */
inline fun <T> expect(expected: T, block: ()-> T) {
    expect(expected, block.toString(), block)
}

/** Asserts that given function block returns the given expected value and use the given message if it fails */
inline fun <T> expect(expected: T, message: String, block: ()-> T) {
    val actual = block()
    assertEquals(expected, actual, message)
}

/** Asserts that given function block fails by throwing an exception */
fun fails(block: ()-> Unit): Throwable? {
    try {
        block()
        asserter.fail("Expected an exception to be thrown")
        return null
    } catch (e: Throwable) {
        //println("Caught expected exception: $e")
        return e
    }
}

/** Asserts that a block fails with a specific exception being thrown */
fun <T: Throwable> failsWith(block: ()-> Unit) {
    try {
        block()
        asserter.fail("Expected an exception to be thrown")
    } catch (e: T) {
        //println("Caught expected exception: $e")
        // OK
    }
}

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
inline fun todo(block: ()-> Any) {
    println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
}

/**
 * A plugin for performing assertions which can reuse JUnit or TestNG
 */
trait Asserter {
    fun assertTrue(message: String, actual: Boolean): Unit

    fun assertEquals(message: String, expected: Any?, actual: Any?): Unit

    fun assertNotNull(message: String, actual: Any?): Unit

    fun assertNull(message: String, actual: Any?): Unit

    fun fail(message: String): Unit
}

/**
 * Default implementation to avoid dependency on JUnit or TestNG
 */
class DefaultAsserter() : Asserter {

    override fun assertTrue(message : String, actual : Boolean) {
        if (!actual) {
            fail(message)
        }
    }

    override fun assertEquals(message : String, expected : Any?, actual : Any?) {
        if (expected != actual) {
            fail("$message. Expected <$expected> actual <$actual>")
        }
    }

    override fun assertNotNull(message : String, actual : Any?) {
        if (actual == null) {
            fail(message)
        }
    }

    override fun assertNull(message : String, actual : Any?) {
        if (actual != null) {
            fail(message)
        }
    }
    override fun fail(message : String) {
        // TODO work around compiler bug as it should never try call the private constructor
        throw AssertionError(message as Object)
    }
}