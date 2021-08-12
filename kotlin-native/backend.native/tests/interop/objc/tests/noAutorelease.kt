import kotlinx.cinterop.*
import kotlin.native.ref.*
import kotlin.test.*
import objcTests.*

private fun <T> testSend(
        createObject: () -> T,
        kotlinPeerRetainsObjC: Boolean = true,
        sendObject: NoAutoreleaseHelperProtocol.(T) -> Unit
) = repeat(2) {
    val helper1 = getNoAutoreleaseHelperImpl()!!
    val helper2 = getNoAutoreleaseHelperImpl()!!

    val weakRef = runNoInline {
        val obj = createObject()

        helper1.sendObject(obj)
        helper2.sendObject(obj)

        WeakReference(obj)
    }

    runNoInline {
        assertNotNull(weakRef.value)
        assertEquals(!kotlinPeerRetainsObjC, helper1.weakIsNull())
        assertEquals(!kotlinPeerRetainsObjC, helper2.weakIsNull())
    }

    kotlin.native.internal.GC.collect()

    runNoInline {
        assertNull(weakRef.value)
        assertTrue(helper1.weakIsNull())
        assertTrue(helper2.weakIsNull())
    }
}

private fun <T> testReceive(
        kotlinPeerRetainsObjC: Boolean = true, // FIXME: is it used?
        receiveObject: NoAutoreleaseHelperProtocol.() -> T
) = repeat(2) {
    val helper = getNoAutoreleaseHelperImpl()!!

    val (weakRef1, weakRef2) = runNoInline {
        val obj1 = helper.receiveObject()
        val obj2 = helper.receiveObject()

        helper.clear()

        Pair(WeakReference(obj1), WeakReference(obj2))
    }

    runNoInline {
        assertNotNull(weakRef1.value)
        assertNotNull(weakRef2.value)
        assertEquals(!kotlinPeerRetainsObjC, helper.weakIsNull())
    }

    kotlin.native.internal.GC.collect()

    runNoInline {
        assertNull(weakRef1.value)
        assertNull(weakRef2.value)
        assertTrue(helper.weakIsNull())
    }
}

private class KotlinObject

@Test fun testSendObject() = testSend({ KotlinObject() }) {
    sendObject(it)
}

@Test
fun testSameObject() {
    val obj = KotlinObject()
    testReceive {
        sameObject(obj)
    }
}

@Test fun testSendCustomObject() = testSend({ NoAutoreleaseCustomObject() }) {
    sendCustomObject(it)
}

@Test fun testReceiveCustomObject() = testReceive {
    receiveCustomObject()
}

@Test fun testSendArray() = testSend({ listOf(KotlinObject()) }, kotlinPeerRetainsObjC = false) {
    sendArray(it)
}

@Test fun testReceiveArray() = testReceive {
    receiveArray()
}

@Test fun testSendString() = testSend({ Any().toString() }) {
    sendString(it)
}

@Test fun testReceiveString() = testReceive {
    receiveString()
}

@Test fun testSendBlock() = testSend({
    val lambdaResult = 123
    { lambdaResult } // make it capturing
}, kotlinPeerRetainsObjC = false) {
    sendBlock(it)
}

@Test fun testReceiveBlock() = testReceive {
    receiveBlock()
}

// Note: this executes code with a separate stack frame,
//   so no stack refs will remain after it, and GC will be able to collect the garbage.
private fun <R> runNoInline(block: () -> R) = block()
