package kotlin.test

import kotlin.test.*

fun init() {
    asserter = JsTestsAsserter()
}

public class JsTestsAsserter() : Asserter {
    public override fun assertTrue(message: String, actual: Boolean) {
        assert(actual, message)
    }
    public override fun assertEquals(message: String, expected: Any?, actual: Any?) {
        assert(actual == expected, "$message. Expected <$expected> actual <$actual>")
    }
    public override fun assertNotNull(message: String, actual: Any?) {
        assert(actual != null, message)
    }
    public override fun assertNull(message: String, actual: Any?) {
        assert(actual == null, message)
    }
    public override fun fail(message: String) {
        assert(false, message)
    }
}

native("JsTests.assert")
public fun assert(value: Boolean, message: String): Unit = noImpl
