package kotlin.test

import kotlin.test.*

fun init() {
    asserter = JsTestsAsserter()
}

public class JsTestsAsserter() : Asserter {
    public override fun fail(message: String?): Nothing = failWithMessage(message)
}

@native("JsTests.assert")
public fun assert(value: Boolean, message: String?): Unit = noImpl

@native("JsTests.fail")
private fun failWithMessage(message: String?): Nothing = noImpl
