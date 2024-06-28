// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: JS

var demoCallCounter = 0

fun flushDemoCallCounter() = demoCallCounter.also { demoCallCounter = 0 }

fun demo(): Unit { ++demoCallCounter }

inline fun inlineDemo(): Unit {
    demo()
}

// CHECK_CALLED_IN_SCOPE: scope=testUsualCall function=Unit_getInstance
fun testUsualCall() {
    val x = demo()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallNoVar function=Unit_getInstance
fun testUsualCallNoVar() {
    demo()
    assertEquals(flushDemoCallCounter(), 1)
}

// CHECK_CALLED_IN_SCOPE: scope=testUsualCallInReturn function=Unit_getInstance
fun testUsualCallInReturn() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInReturn$test function=Unit_getInstance
    fun test() { return demo() }
    val x = test()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInReturnNoVar function=Unit_getInstance
fun testUsualCallInReturnNoVar() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInReturnNoVar$test function=Unit_getInstance
    fun test() { return demo() }
    test()
    assertEquals(flushDemoCallCounter(), 1)
}

// CHECK_CALLED_IN_SCOPE: scope=testUsualCallInExpressionBody function=Unit_getInstance
fun testUsualCallInExpressionBody() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInExpressionBody$test function=Unit_getInstance
    fun test() = demo()
    val x = test()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInExpressionBodyNoVar function=Unit_getInstance
fun testUsualCallInExpressionBodyNoVar() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallInExpressionBodyNoVar$test function=Unit_getInstance
    fun test() = demo()
    test()
    assertEquals(flushDemoCallCounter(), 1)
}

// CHECK_CALLED_IN_SCOPE: scope=testInlineCall function=Unit_getInstance
fun testInlineCall() {
    val x = inlineDemo()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testUsualCallNoVar function=Unit_getInstance
fun testInlineCallNoVar() {
    inlineDemo()
    assertEquals(flushDemoCallCounter(), 1)
}

// CHECK_CALLED_IN_SCOPE: scope=testInlineCallInReturn function=Unit_getInstance
fun testInlineCallInReturn() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInReturn$test function=Unit_getInstance
    fun test() { return inlineDemo() }
    val x = test()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInReturnNoVar function=Unit_getInstance
fun testInlineCallInReturnNoVar() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInReturnNoVar$test function=Unit_getInstance
    fun test() { return inlineDemo() }
    test()
    assertEquals(flushDemoCallCounter(), 1)
}

// CHECK_CALLED_IN_SCOPE: scope=testInlineCallInExpressionBody function=Unit_getInstance
fun testInlineCallInExpressionBody() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInExpressionBody$test function=Unit_getInstance
    fun test() = inlineDemo()
    val x = test()
    assertEquals(flushDemoCallCounter(), 1)
    assertEquals(x.toString(), "kotlin.Unit")
}

// CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInExpressionBodyNoVar function=Unit_getInstance
fun testInlineCallInExpressionBodyNoVar() {
    // CHECK_NOT_CALLED_IN_SCOPE: scope=testInlineCallInExpressionBodyNoVar$test function=Unit_getInstance
    fun test() = inlineDemo()
    test()
    assertEquals(flushDemoCallCounter(), 1)
}

fun box(): String {
    testUsualCall()
    testUsualCallNoVar()
    testUsualCallInReturn()
    testUsualCallInReturnNoVar()
    testUsualCallInExpressionBody()
    testUsualCallInExpressionBodyNoVar()

    testInlineCall()
    testInlineCallNoVar()
    testInlineCallInReturn()
    testInlineCallInReturnNoVar()
    testInlineCallInExpressionBody()
    testInlineCallInExpressionBodyNoVar()

    return "OK"
}
