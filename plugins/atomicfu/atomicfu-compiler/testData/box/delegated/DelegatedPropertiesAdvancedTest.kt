// DUMP_IR_DIFFERENCE: NATIVE, JS_IR
// ISSUE: KT-90507
// LANGUAGE: +CompanionBlocks
// FILE: Containers.kt
import kotlin.reflect.KMutableProperty0
import kotlin.test.*
import kotlinx.atomicfu.atomic

inline fun testBoolProperty(property: KMutableProperty0<Boolean>, getAtomicValue: () -> Boolean, setAtomicValue: (Boolean) -> Unit) {
    setAtomicValue(false)
    assertFalse(property.get())
    property.set(true)
    assertTrue(getAtomicValue())
    setAtomicValue(false)
    assertFalse(property.get())
}

inline fun testIntProperty(property: KMutableProperty0<Int>, resetAtomic: () -> Unit, getAtomicValue: () -> Int, incrementAtomic: () -> Unit) {
    resetAtomic()
    assertEquals(0, property.get())
    property.set(1)
    assertEquals(1, getAtomicValue())
    incrementAtomic()
    assertEquals(2, property.get())
}

inline fun testRefProperty(property: KMutableProperty0<Any?>, getAtomicValue: () -> Any?, setAtomicValue: (Any?) -> Unit) {
    setAtomicValue(null)
    assertNull(property.get())
    property.set("Hello World!")
    assertEquals("Hello World!", getAtomicValue())
    setAtomicValue("What's up?")
    assertEquals("What's up?", property.get())
}

enum class Enum {
    FOO,
    BAR {
        private val barAtomic = atomic(0)
        override var delegated: Int by barAtomic
        override var delegatedWithThis by this.barAtomic
        override var delegatedWithExplicitThis by this@BAR.barAtomic

        override fun testCase() {
            for (property in arrayOf(::delegated, ::delegatedWithThis, ::delegatedWithExplicitThis)) {
                testIntProperty(property, { barAtomic.value = 0 }, { barAtomic.value }, { barAtomic.incrementAndGet() })
            }
        }
    };

    private val atomicProperty = atomic(0)
    open var delegated: Int by atomicProperty
    open var delegatedWithThis by this.atomicProperty
    open var delegatedWithExplicitThis by this@Enum.atomicProperty

    open fun testCase() {
        for (property in arrayOf(::delegated, ::delegatedWithThis, ::delegatedWithExplicitThis)) {
            testIntProperty(property, { atomicProperty.value = 0 }, { atomicProperty.value }, { atomicProperty.incrementAndGet() })
        }
    }

    companion {
        fun test() {
            Enum.FOO.testCase()
            Enum.BAR.testCase()
        }
    }
}

class SameContainer {
    private val atomicProperty = atomic(0)
    var delegated: Int by atomicProperty
    var delegatedWithThis by this.atomicProperty
    var delegatedWithExplicitThis by this@SameContainer.atomicProperty

    fun test() {
        for (property in arrayOf(::delegated, ::delegatedWithThis, ::delegatedWithExplicitThis)) {
            testIntProperty(property, { atomicProperty.value = 0 }, { atomicProperty.value }, { atomicProperty.incrementAndGet() })
        }
    }
}

class SameContainerCompanionObject {
    companion object {
        private val atomicProperty = atomic(0)
        var delegated: Int by atomicProperty
        var delegatedWithThis by this.atomicProperty
        var delegatedExplicit by SameContainerCompanionObject.atomicProperty

        fun test() {
            for (property in arrayOf(::delegated, ::delegatedWithThis, ::delegatedExplicit)) {
                testIntProperty(property, { atomicProperty.value = 0 }, { atomicProperty.value }, { atomicProperty.incrementAndGet() })
            }
        }
    }
}

object SameContainerObject {
    private val atomicProperty = atomic(0)
    var delegated: Int by atomicProperty
    var delegatedExplicit by SameContainerObject.atomicProperty

    fun test() {
        for (property in arrayOf(::delegated, ::delegatedExplicit)) {
            testIntProperty(property, { atomicProperty.value = 0 }, { atomicProperty.value }, { atomicProperty.incrementAndGet() })
        }
    }
}

class SameContainerCompanionBlock {
    companion {
        private val atomicBool = atomic(false)
        private val atomicInt = atomic(0)
        private val atomicReference = atomic<Any?>(null)
        var delegated: Int by atomicInt
        var delegatedExplicit by SameContainerCompanionBlock.atomicInt
    }

    companion {
        var secondDelegated: Int by atomicInt
        var secondDelegatedExplicit by SameContainerCompanionBlock.atomicInt
    }

    var delegatedBool by atomicBool
    var delegatedInt by atomicInt
    var delegatedIntExplicit by SameContainerCompanionBlock.atomicInt
    var delegatedReference by atomicReference

    fun test() {
        val properties = arrayOf(
            this::delegatedInt, this::delegatedIntExplicit, SameContainerCompanionBlock::delegated,
            SameContainerCompanionBlock::delegatedExplicit, SameContainerCompanionBlock::secondDelegated,
            SameContainerCompanionBlock::secondDelegatedExplicit,
        )

        for (property in properties) {
            testIntProperty(property, { atomicInt.value = 0 }, { atomicInt.value }, { atomicInt.incrementAndGet() })
        }
        testBoolProperty(this::delegatedBool, { atomicBool.value }, { atomicBool.value = it })
        testRefProperty(this::delegatedReference, { atomicReference.value }, { atomicReference.value = it })
    }
}

fun testNestedType() {
    class Nested {
        val atomicProperty = atomic(0)
        var delegated by atomicProperty
    }

    val n = Nested()

    testIntProperty(n::delegated, { n.atomicProperty.value = 0 }, { n.atomicProperty.value }, { n.atomicProperty.incrementAndGet() })
}

fun box(): String {
    Enum.test()
    SameContainer().test()
    SameContainerCompanionObject.test()
    SameContainerObject.test()
    SameContainerCompanionBlock().test()
    testNestedType()

    return "OK"
}
