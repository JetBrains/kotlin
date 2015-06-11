package test.properties.delegation.lazy

import test.properties.delegation.WithBox
import test.properties.delegation.DelegationTestBase
import org.junit.Test as test
import kotlin.properties.*

class LazyValuesTest(): DelegationTestBase() {

    test fun testLazyVal() {
        doTest(TestLazyVal())
    }

    test fun testUnsafeLazyVal() {
        doTest(TestUnsafeLazyVal())
    }

    test fun testNullableLazyVal() {
        doTest(TestNullableLazyVal())
    }

    test fun testUnsafeNullableLazyVal() {
        doTest(TestUnsafeNullableLazyVal())
    }

    // deprecated lazy tests

    test fun testUnsafeLazyValDeprecated() {
        doTest(TestUnsafeLazyValDeprecated())
    }

    test fun testBlockingLazyValDeprecated() {
        doTest(TestBlockingLazyValDeprecated())
    }

    test fun testUnsafeNullableLazyValDeprecated() {
        doTest(TestUnsafeNullableLazyValDeprecated())
    }

    test fun testBlockingNullableLazyValDeprecated() {
        doTest(TestBlockingNullableLazyValDeprecated())
    }

    test fun testIdentityEqualsIsUsedToUnescapeLazyVal() {
        doTest(TestIdentityEqualsIsUsedToUnescapeLazyVal())
    }
}

class TestLazyVal: WithBox {
    var result = 0
    val a by lazy {
        ++result
    }

    override fun box(): String {
        a
        if (a != 1) return "fail: initializer should be invoked only once"
        return "OK"
    }
}

class TestUnsafeLazyVal: WithBox {
    var result = 0
    val a by lazy(LazyThreadSafetyMode.NONE) {
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

    val a: Int? by lazy { resultA++; null}
    val b by lazy { foo() }

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

class TestUnsafeNullableLazyVal: WithBox {
    var resultA = 0
    var resultB = 0

    val a: Int? by lazy(LazyThreadSafetyMode.NONE) { resultA++; null}
    val b by lazy(LazyThreadSafetyMode.NONE) { foo() }

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

class TestUnsafeLazyValDeprecated: WithBox {
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

class TestBlockingLazyValDeprecated: WithBox {
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

class TestUnsafeNullableLazyValDeprecated : WithBox {
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

class TestBlockingNullableLazyValDeprecated: WithBox {
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

class TestIdentityEqualsIsUsedToUnescapeLazyVal: WithBox {
    var equalsCalled = 0
    val a by lazy { ClassWithCustomEquality { equalsCalled++ } }

    override fun box(): String {
        a
        a
        if (equalsCalled > 0) return "fail: equals called $equalsCalled times."
        return "OK"
    }
}

private class ClassWithCustomEquality(private val onEqualsCalled: () -> Unit) {
    override fun equals(other: Any?): Boolean {
        onEqualsCalled()
        return super.equals(other)
    }
}