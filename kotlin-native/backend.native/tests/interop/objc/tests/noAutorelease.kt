import kotlinx.cinterop.*
import kotlin.native.internal.GC
import kotlin.native.ref.*
import kotlin.test.*
import objcTests.*

// The tests below make best efforts to ensure that objects don't "leak" to autoreleasepool
// when simply passing them between Kotlin and Objective-C.

private class KotlinLivenessTracker {
    val refs = mutableListOf<WeakReference<Any>>()

    fun add(obj: Any) {
        refs += WeakReference(obj)
    }

    fun isEmpty() = refs.isEmpty()
    fun objectsAreAlive() = refs.all { it.value !== null }
    fun objectsAreDead() = refs.all { it.value === null }
}

private fun test(
        kotlinPeerRetainsObjC: Boolean = true,
        block: (kotlinLivenessTracker: KotlinLivenessTracker, objCLivenessTracker: ObjCLivenessTracker) -> Unit
) = repeat(2) {
    val kotlinLivenessTracker = KotlinLivenessTracker()
    val objCLivenessTracker = ObjCLivenessTracker()

    GC.collect() // Make predictable

    autoreleasepool { // FIXME: remove!!
    block(kotlinLivenessTracker, objCLivenessTracker)
    }

    assertFalse(kotlinLivenessTracker.isEmpty())
    assertTrue(kotlinLivenessTracker.objectsAreAlive())

    assertFalse(objCLivenessTracker.isEmpty())
    if (kotlinPeerRetainsObjC) {
        assertTrue(objCLivenessTracker.objectsAreAlive())
    } else {
        assertTrue(objCLivenessTracker.objectsAreDead())
    }

    GC.collect()

    assertTrue(kotlinLivenessTracker.objectsAreDead())
    assertTrue(objCLivenessTracker.objectsAreDead())

    // TODO: can we create an autoreleasepool and actually check that it is not used?
}

private fun <T> testSendToObjC(
        createObject: () -> T,
        kotlinPeerRetainsObjC: Boolean = true,
        sendObject: NoAutoreleaseHelperProtocol.(T) -> Unit
) = test(kotlinPeerRetainsObjC) { kotlinLivenessTracker, objCLivenessTracker ->
    val helper = getNoAutoreleaseHelperImpl(objCLivenessTracker)!!

    val obj = createObject()!!
    kotlinLivenessTracker.add(obj)

    helper.sendObject(obj)
    helper.sendObject(obj)
}

private fun <T> testReceiveFromObjC(
        kotlinPeerRetainsObjC: Boolean = true, // FIXME: is it used?
        receiveObject: NoAutoreleaseHelperProtocol.() -> T
) = test(kotlinPeerRetainsObjC) { kotlinLivenessTracker, objCLivenessTracker ->
    val helper = getNoAutoreleaseHelperImpl(objCLivenessTracker)!!

    val obj1 = helper.receiveObject()!!
    val obj2 = helper.receiveObject()!!

    kotlinLivenessTracker.add(obj1)
    kotlinLivenessTracker.add(obj2)
}

private fun testCallToKotlin(
        kotlinPeerRetainsObjC: Boolean = true, // FIXME: is it used?
        callObjC: NoAutoreleaseHelperProtocol.(ObjCLivenessTracker) -> Unit
) = test { kotlinLivenessTracker, objCLivenessTracker ->

    val helper = object : NSObject(), NoAutoreleaseHelperProtocol {
        val myKotlinObject = KotlinObject4()
        val objCObject = NoAutoreleaseCustomObject()
        val array = listOf(Any())
        val string = Any().toString()
        val block = createLambda()

        override fun kotlinObject(): Any? {
            error("should not be used")
        }

        override fun setKotlinObject(value: Any?) {
            error("should not be used")
        }

        override fun sendKotlinObject(kotlinObject: Any?) {
            kotlinLivenessTracker.add(kotlinObject!!)
        }

        override fun receiveKotlinObject(): Any? {
            val result = myKotlinObject
            kotlinLivenessTracker.add(result)
            return result
        }

        override fun sendObjCObject(objCObject: NoAutoreleaseCustomObject?) {
            kotlinLivenessTracker.add(objCObject!!)
        }

        override fun receiveObjCObject(): NoAutoreleaseCustomObject? {
            val result = objCObject
            kotlinLivenessTracker.add(result)
            return result
        }

        override fun sendArray(array: List<*>?) {
            kotlinLivenessTracker.add(array!!)
        }

        override fun receiveArray(): List<*>? {
            val result = array
            kotlinLivenessTracker.add(result)
            return result
        }

        override fun sendString(string: String?) {
            kotlinLivenessTracker.add(string!!)
        }

        override fun receiveString(): String? {
            val result = string
            kotlinLivenessTracker.add(result)
            return result
        }

        override fun sendBlock(block: (() -> Int)?) {
            kotlinLivenessTracker.add(block!!)
        }

        override fun receiveBlock(): (() -> Int)? {
            val result = block
            kotlinLivenessTracker.add(result)
            return result
        }
    }

    helper.callObjC(objCLivenessTracker)
}

private class KotlinObject1
private class KotlinObject2
private class KotlinObject3
private class KotlinObject4

@Test fun testSendKotlinObjectToObjC() = testSendToObjC({ KotlinObject1() }) {
    sendKotlinObject(it)
}

@Test fun testReceiveKotlinObjectFromObjC() {
    val obj = KotlinObject2()
    testReceiveFromObjC {
        this.kotlinObject = obj
        receiveKotlinObject()
    }
}

@Test fun testSendObjCObjectToObjC() = testSendToObjC({ NoAutoreleaseCustomObject() }) {
    sendObjCObject(it)
}

@Test fun testReceiveObjCObjectFromObjC() = testReceiveFromObjC {
    receiveObjCObject()
}

@Test fun testSendArrayToObjC() = testSendToObjC({ listOf(KotlinObject1()) }, kotlinPeerRetainsObjC = false) {
    sendArray(it)
}

@Test fun testReceiveArrayFromObjC() = testReceiveFromObjC {
    receiveArray()
}

@Test fun testSendStringToObjC() = testSendToObjC({ Any().toString() }) {
    sendString(it)
}

@Test fun testReceiveStringFromObjC() = testReceiveFromObjC {
    receiveString()
}

@Test fun testSendBlockToObjC() = testSendToObjC({ createLambda() }, kotlinPeerRetainsObjC = false) {
    sendBlock(it)
}

@Test fun testReceiveBlockFromObjC() = testReceiveFromObjC {
    receiveBlock()
}

@Test fun testSendKotlinObjectToKotlin() = testCallToKotlin {
    callSendKotlinObject(this, KotlinObject3(), it)
}

@Test fun testReceiveKotlinObjectFromKotlin() = testCallToKotlin {
    callReceiveKotlinObject(this, it)
}

@Test fun testSendObjCObjectToKotlin() = testCallToKotlin {
    callSendObjCObject(this, it)
}

@Test fun testReceiveObjCObjectFromKotlin() = testCallToKotlin {
    callReceiveObjCObject(this, it)
}

@Test fun testSendArrayToKotlin() = testCallToKotlin {
    callSendArray(this, it)
}

@Test fun testReceiveArrayFromKotlin() = testCallToKotlin {
    callReceiveArray(this, it)
}

@Test fun testSendStringToKotlin() = testCallToKotlin {
    callSendString(this, it)
}

@Test fun testReceiveStringFromKotlin() = testCallToKotlin {
    callReceiveString(this, it)
}

@Test fun testSendBlockToKotlin() = testCallToKotlin {
    callSendBlock(this, it)
}

@Test fun testReceiveBlockFromKotlin() = testCallToKotlin {
    callReceiveBlock(this, it)
}

private fun createLambda(): () -> Int {
    val lambdaResult = 123
    return { lambdaResult } // make it capturing
}

// FIXME: remove
// Note: this executes code with a separate stack frame,
//   so no stack refs will remain after it, and GC will be able to collect the garbage.
private fun <R> runNoInline(block: () -> R) = block()
