// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-atomicfu-1.jar

import kotlinx.atomicfu.*
import kotlin.test.*
import kotlin.reflect.*

private class AAA {
    val _a = atomic(67)
    var intVal: Int = 77
}

inline fun AtomicInt.foo() {
    val intialValue = value
    assertEquals(intialValue, value)
    value = 56
    assertEquals(56, getAndSet(77))
    assertEquals(77, value)
    innerFoo(value)
    assertEquals(1000, value)
}

inline fun AtomicInt.innerFoo(currentValue: Int) {
    compareAndSet(currentValue, 1000)
}

@Test
fun testAtomicExtension() {
    val aClass = AAA()
    aClass._a.foo()
}
