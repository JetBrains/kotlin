import kotlinx.cinterop.*
import kotlin.native.ref.*
import kotlin.test.*
import objcTests.*

private fun ensureNoAutorelease(kotlinPeerRetainsObjC: Boolean = true, createAndUseObject: NoAutoreleaseHelperProtocol.() -> Any?) {
    val helper = getNoAutoreleaseHelperImpl()!!

    val weakRef = runNoInline {
        val obj = helper.createAndUseObject()

        // Ensure the test checks what it was intended to
        assertNotSame(Unit, obj)
        assertNotNull(obj)

        WeakReference(obj)
    }

    runNoInline {
        assertNotNull(weakRef.value)
        assertEquals(!kotlinPeerRetainsObjC, helper.weakIsNull())
        Unit
    }

    kotlin.native.internal.GC.collect()

    runNoInline {
        assertNull(weakRef.value)
        assertTrue(helper.weakIsNull())
    }
}

private class KotlinObject

@Test fun testSendObject() = ensureNoAutorelease {
    KotlinObject().also {
        sendObject(it)
    }
}

@Test fun testSameObject() = ensureNoAutorelease {
    sameObject(KotlinObject())
}

@Test fun testSendCustomObject() = ensureNoAutorelease {
    NoAutoreleaseCustomObject().also {
        sendCustomObject(it)
    }
}

@Test fun testReceiveCustomObject() = ensureNoAutorelease {
    receiveCustomObject()
}

@Test fun testSendArray() = ensureNoAutorelease(kotlinPeerRetainsObjC = false) {
    listOf(KotlinObject()).also {
        sendArray(it)
    }
}

@Test fun testReceiveArray() = ensureNoAutorelease {
    receiveArray()
}

@Test fun testSendString() = ensureNoAutorelease {
    buildString {
        append(Any())
    }.also {
        sendString(it)
    }
}

@Test fun testReceiveString() = ensureNoAutorelease {
    receiveString()
}

@Test fun testSendBlock() = ensureNoAutorelease(kotlinPeerRetainsObjC = false) {
    val lambdaResult = 123
    { 123 }.also {
        sendBlock(it)
    }
}

@Test fun testReceiveBlock() = ensureNoAutorelease {
    receiveBlock()
}

// Note: this executes code with a separate stack frame,
//   so no stack refs will remain after it, and GC will be able to collect the garbage.
private fun <R> runNoInline(block: () -> R) = block()
