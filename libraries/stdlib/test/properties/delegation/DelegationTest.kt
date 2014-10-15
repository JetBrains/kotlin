package test.properties.delegation

import org.junit.Test as test
import kotlin.test.*
import kotlin.properties.*

trait WithBox {
    fun box(): String
}

abstract class DelegationTestBase {
    fun doTest(klass: WithBox) {
        assertEquals("OK", klass.box())
    }
}

class DelegationTest(): DelegationTestBase() {
    test fun testNotNullVar() {
        doTest(TestNotNullVar("a", "b"))
    }

    test fun testObservablePropertyInChangeSupport() {
        doTest(TestObservablePropertyInChangeSupport())
    }

    test fun testObservableProperty() {
        doTest(TestObservableProperty())
    }

    test fun testVetoableProperty() {
        doTest(TestVetoableProperty())
    }
}

public class TestNotNullVar<T>(val a1: String, val b1: T): WithBox {
    var a: String by Delegates.notNull()
    var b by Delegates.notNull<T>()

    override fun box(): String {
        a = a1
        b = b1
        if (a != "a") return "fail: a shouuld be a, but was $a"
        if (b != "b") return "fail: b should be b, but was $b"
        return "OK"
    }
}

class TestObservablePropertyInChangeSupport: WithBox, ChangeSupport() {

    var b by property(init = 2)
    var c by property(3)

    override fun box(): String {
        var result = false
        addChangeListener("b", object: ChangeListener {
            public override fun onPropertyChange(event: ChangeEvent) {
                result = true
            }
        })
        addChangeListener("c", object: ChangeListener {
            public override fun onPropertyChange(event: ChangeEvent) {
                result = false
            }
        })
        b = 4
        if (b != 4) return "fail: b != 4"
        if (!result) return "fail: result should be true"
        return "OK"
    }
}

class TestObservableProperty: WithBox {
    var result = false

    var b by Delegates.observable(1, {(pd, o, n) -> result = true})

    override fun box(): String {
        b = 4
        if (b != 4) return "fail: b != 4"
        if (!result) return "fail: result should be true"
        return "OK"
    }
}

class A(val p: Boolean)

class TestVetoableProperty: WithBox {
    var result = false
    var b by Delegates.vetoable(A(true), {(pd, o, n) -> result = n.p == true; result})

    override fun box(): String {
        val firstValue = A(true)
        b = firstValue
        if (b != firstValue) return "fail1: b should be firstValue = A(true)"
        if (!result) return "fail2: result should be true"
        b = A(false)
        if (b != firstValue) return "fail3: b should be firstValue = A(true)"
        if (result) return "fail4: result should be false"
        return "OK"
    }
}
