import kotlinx.cinterop.*
import kotlin.native.ref.*
import kotlin.test.*
import objcTests.*

@Test fun testConvertingLambdaToBlockDoesntUseAutorelease() {
    val lambdaResult = 123
    val lambdaWeakRef = runNoInline {
        val lambda = { lambdaResult }
        assertEquals(123, callLambdaAsBlock(lambda))
        WeakReference(lambda)
    }

    runNoInline {
        assertNotNull(lambdaWeakRef.value)
        Unit
    }

    kotlin.native.internal.GC.collect()

    runNoInline {
        assertNull(lambdaWeakRef.value)
    }
}


// Note: this executes code with a separate stack frame,
//   so no stack refs will remain after it, and GC will be able to collect the garbage.
private fun <R> runNoInline(block: () -> R) = block()
