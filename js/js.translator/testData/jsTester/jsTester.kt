package kotlin.test

fun init() {
    asserter = JsTestsAsserter()
}

public class JsTestsAsserter() : Asserter {
    public override fun fail(message: String?): Nothing = failWithMessage(message)
}

@JsName("JsTests.assert")
public external fun assert(value: Boolean, message: String?): Unit = noImpl

@JsName("JsTests.fail")
private external fun failWithMessage(message: String?): Nothing = noImpl
