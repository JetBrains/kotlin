// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

class TraceTest {
    companion {
        private val defaultTrace = Trace()
        private val a1 = atomic(5, defaultTrace)

        fun testCompanionTrace() {
            val oldValue = a1.value
            defaultTrace { "before CAS value = $oldValue" }
            val res = a1.compareAndSet(oldValue, oldValue * 10)
            val newValue = a1.value
            defaultTrace { "after CAS value = $newValue" }
        }
    }
}

private companion val TraceTest.extTrace = Trace()
private companion val TraceTest.a = atomic(42, TraceTest.extTrace)

fun testExtensionTrace() {
    val oldValue = TraceTest.a.value
    TraceTest.extTrace { "before CAS value = $oldValue" }
    val res = TraceTest.a.compareAndSet(oldValue, oldValue * 10)
    val newValue = TraceTest.a.value
    TraceTest.extTrace { "after CAS value = $newValue" }
}

private fun test() {
    TraceTest.testCompanionTrace()
    testExtensionTrace()
}

fun box(): String {
    test()
    return "OK"
}

