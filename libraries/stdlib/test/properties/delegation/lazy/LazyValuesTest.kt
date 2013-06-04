package test.properties.delegation.lazy

import test.properties.delegation.WithBox
import test.properties.delegation.DelegationTestBase
import kotlin.properties.*

class LazyValuesTest(): DelegationTestBase() {

    fun testLazyVal() {
        doTest(TestLazyVal())
    }

    fun testNullableLazyVal() {
        doTest(TestNullableLazyVal())
    }

    fun testAtomicNullableLazyVal() {
        doTest(TestAtomicNullableLazyVal())
    }

    fun testAtomicLazyVal() {
        doTest(TestAtomicLazyVal())
    }

    fun testVolatileNullableLazyVal() {
        doTest(TestVolatileNullableLazyVal())
    }

    fun testVolatileLazyVal() {
        doTest(TestVolatileLazyVal())
    }
}

class TestLazyVal: WithBox {
    var result = 0
    val a by Delegates.lazy {
        ++result
    }

    override fun box(): String {
        a
        if (a != 1) return "fail: initializer should be invoked only once"
        return "OK"
    }
}

class TestNullableLazyVal: WithBox {
    var resultA = 0
    var resultB = 0

    val a: Int? by Delegates.lazy { resultA++; null}
    val b by Delegates.lazy { foo() }

    override fun box(): String {
        a
        b

        if (a != null) return "fail: a should be null"
        if (b != null) return "fail: a should be null"
        if (resultA != 1) return "fail: initializer for a should be invoked only once"
        if (resultB != 1) return "fail: initializer for b should be invoked only once"
        return "OK"
    }

    fun foo(): String? {
        resultB++
        return null
    }
}

class TestAtomicLazyVal: WithBox {
    var result = 0
    val a by Delegates.blockingLazy {
        ++result
    }

    override fun box(): String {
        a
        if (a != 1) return "fail: initializer should be invoked only once"
        return "OK"
    }
}

class TestVolatileNullableLazyVal: WithBox {
    var resultA = 0
    var resultB = 0

    val a: Int? by Delegates.blockingLazy { resultA++; null}
    val b by Delegates.blockingLazy { foo() }

    override fun box(): String {
        a
        b

        if (a != null) return "fail: a should be null"
        if (b != null) return "fail: a should be null"
        if (resultA != 1) return "fail: initializer for a should be invoked only once"
        if (resultB != 1) return "fail: initializer for b should be invoked only once"
        return "OK"
    }

    fun foo(): String? {
        resultB++
        return null
    }
}

class TestVolatileLazyVal: WithBox {
    var result = 0
    val a by Delegates.blockingLazy {
        ++result
    }

    override fun box(): String {
        a
        if (a != 1) return "fail: initializer should be invoked only once"
        return "OK"
    }
}

class TestAtomicNullableLazyVal: WithBox {
    var resultA = 0
    var resultB = 0

    val a: Int? by Delegates.blockingLazy { resultA++; null}
    val b by Delegates.blockingLazy { foo() }

    override fun box(): String {
        a
        b

        if (a != null) return "fail: a should be null"
        if (b != null) return "fail: a should be null"
        if (resultA != 1) return "fail: initializer for a should be invoked only once"
        if (resultB != 1) return "fail: initializer for b should be invoked only once"
        return "OK"
    }

    fun foo(): String? {
        resultB++
        return null
    }
}
