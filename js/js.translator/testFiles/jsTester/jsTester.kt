package kotlin.test

import kotlin.test.*
import js.*

public var asserter : Asserter = JsTestsAsserter()

native("JsTests.Asserter")
public class JsTestsAsserter(): Asserter {

    native
    public override fun assertTrue(message: String, actual: Boolean) = noImpl

    native
    public override fun assertEquals(message: String, expected: Any?, actual: Any?) = noImpl

    native
    public override fun assertNotNull(message: String, actual: Any?) = noImpl

    native
    public override fun assertNull(message: String, actual: Any?) = noImpl

    native
    public override fun fail(message: String) = noImpl
}