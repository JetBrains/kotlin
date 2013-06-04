package test.properties.delegation

import junit.framework.TestCase
import kotlin.test.*
import kotlin.properties.*

trait WithBox {
    fun box(): String
}

abstract class DelegationTestBase: TestCase() {
    fun doTest(klass: WithBox) {
        assertEquals("OK", klass.box())
    }
}

class DelegationTest(): DelegationTestBase() {
    fun testNotNullVar() {
        doTest(TestNotNullVar("a", "b"))
    }

    fun testObservableProperty() {
        doTest(TestObservableProperty())
    }
}

public class TestNotNullVar<T>(val a1: String, val b1: T): WithBox {
    var a: String by Delegates.notNull<String>()
    var b by Delegates.notNull<T>()

    override fun box(): String {
        a = a1
        b = b1
        if (a != "a") return "fail: a shouuld be a, but was $a"
        if (b != "b") return "fail: b should be b, but was $b"
        return "OK"
    }
}

class TestObservableProperty: WithBox, ChangeSupport() {

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
