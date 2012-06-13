package kotlin.test

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public inline fun todo(block: ()-> Any) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}


/**
 * Provides the JS implementation of asserter using [QUnit](http://QUnitjs.com/)
 */
public var asserter: Asserter = QUnitAsserter()

public class QUnitAsserter(): Asserter {

    public override fun assertTrue(message: String, actual: Boolean) {
        QUnit.ok(actual, message)
    }

    public override fun assertEquals(message: String, expected: Any?, actual: Any?) {
        QUnit.ok(expected == actual, "$message. Expected <$expected> actual <$actual>")
    }

    public override fun assertNotNull(message: String, actual: Any?) {
        QUnit.ok(actual != null, message)
    }

    public override fun assertNull(message: String, actual: Any?) {
        QUnit.ok(actual == null, message)
    }

    public override fun fail(message: String) {
        QUnit.ok(false, message)
    }
}