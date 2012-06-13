package kotlin.test

import java.util.ServiceLoader

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public inline fun todo(block: ()-> Any) {
    println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
}

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


/**
 * Default implementation to avoid dependency on JUnit or TestNG
 */
class DefaultAsserter() : Asserter {

    public override fun assertTrue(message : String, actual : Boolean) {
        if (!actual) {
            fail(message)
        }
    }

    public override fun assertEquals(message : String, expected : Any?, actual : Any?) {
        if (expected != actual) {
            fail("$message. Expected <$expected> actual <$actual>")
        }
    }

    public override fun assertNotNull(message : String, actual : Any?) {
        if (actual == null) {
            fail(message)
        }
    }

    public override fun assertNull(message : String, actual : Any?) {
        if (actual != null) {
            fail(message)
        }
    }
    public override fun fail(message : String) {
        // TODO work around compiler bug as it should never try call the private constructor
        throw AssertionError(message as Any)
    }
}