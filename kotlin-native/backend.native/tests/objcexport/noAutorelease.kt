package noAutorelease

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.native.internal.NativePtr
import kotlin.native.ref.WeakReference
import kotlin.test.*

class KotlinLivenessTracker {
    val weakRefs = mutableListOf<WeakReference<Any>>()

    fun add(obj: Any?) {
        assertNotNull(obj)
        weakRefs += WeakReference(obj)
        assertFalse(objectsAreDead())
    }

    fun isEmpty() = weakRefs.isEmpty()
    fun objectsAreDead() = weakRefs.all { it.value === null }
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun gc() = kotlin.native.runtime.GC.collect()

class KotlinObject

interface NoAutoreleaseSendHelper {
    fun sendKotlinObject(kotlinObject: KotlinObject)
    fun blockReceivingKotlinObject(): (KotlinObject) -> Unit
    fun sendSwiftObject(swiftObject: Any)
    fun sendList(list: List<*>)
    fun sendString(string: String)
    fun sendNumber(number: Any)
    fun sendBlock(block: () -> KotlinObject)
    suspend fun sendCompletion(): Any?
}

interface NoAutoreleaseReceiveHelper {
    fun receiveKotlinObject(): KotlinObject
    fun receiveSwiftObject(): Any
    fun receiveList(): List<*>
    fun receiveString(): String
    fun receiveNumber(): Any
    fun receiveBlock(): () -> KotlinObject
}

class NoAutoreleaseKotlinSendHelper(val kotlinLivenessTracker: KotlinLivenessTracker) : NoAutoreleaseSendHelper {
    override fun sendKotlinObject(kotlinObject: KotlinObject) = kotlinLivenessTracker.add(kotlinObject)
    override fun blockReceivingKotlinObject(): (KotlinObject) -> Unit = { kotlinLivenessTracker.add(it) }
    override fun sendSwiftObject(swiftObject: Any) = kotlinLivenessTracker.add(swiftObject)
    override fun sendList(list: List<*>) = kotlinLivenessTracker.add(list)
    override fun sendString(string: String) = kotlinLivenessTracker.add(string)
    override fun sendNumber(number: Any) = kotlinLivenessTracker.add(number)
    override fun sendBlock(block: () -> KotlinObject) = kotlinLivenessTracker.add(block)
    override suspend fun sendCompletion() = suspendCoroutineUninterceptedOrReturn<Any?> { continuation ->
        kotlinLivenessTracker.add(continuation)
        null
    }
}

class NoAutoreleaseKotlinReceiveHelper(val kotlinLivenessTracker: KotlinLivenessTracker) : NoAutoreleaseReceiveHelper {
    private val kotlinObject = KotlinObject()
    lateinit var swiftObject: Any
    private val list = listOf(Any())
    private val string = Any().toString()
    private val number = createKotlinNumber()
    private val block = createLambda(kotlinLivenessTracker)

    override fun receiveKotlinObject(): KotlinObject = kotlinObject.also { kotlinLivenessTracker.add(it) }
    override fun receiveSwiftObject(): Any = swiftObject.also { kotlinLivenessTracker.add(it) }
    override fun receiveList(): List<*> = list.also { kotlinLivenessTracker.add(it) }
    override fun receiveString(): String = string.also { kotlinLivenessTracker.add(it) }
    override fun receiveNumber(): Any = number.also { kotlinLivenessTracker.add(it) }
    override fun receiveBlock(): () -> KotlinObject = block.also { kotlinLivenessTracker.add(it) }
}

object NoAutoreleaseSingleton {
    val x = 1
}
enum class NoAutoreleaseEnum {
    ENTRY;

    val x = 2
}

fun callSendKotlinObject(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val kotlinObject = KotlinObject()

    // Repeating twice to cover possible fast paths after caching something for an object.
    helper.sendKotlinObject(kotlinObject)
    helper.sendKotlinObject(kotlinObject)
    tracker.add(kotlinObject)
}

fun sendKotlinObjectToBlock(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val kotlinObject = KotlinObject()

    helper.blockReceivingKotlinObject()(kotlinObject)
    helper.blockReceivingKotlinObject()(kotlinObject)
    tracker.add(kotlinObject)
}

fun callSendSwiftObject(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker, swiftObject: Any) {
    helper.sendSwiftObject(swiftObject)
    helper.sendSwiftObject(swiftObject)
    tracker.add(swiftObject)
}

fun callSendList(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val list = listOf(Any())

    helper.sendList(list)
    helper.sendList(list)
    tracker.add(list)
}

fun callSendString(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val string = Any().toString()

    helper.sendString(string)
    helper.sendString(string)
    tracker.add(string)
}

fun callSendNumber(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val number = createKotlinNumber()

    helper.sendNumber(number)
    helper.sendNumber(number)
    tracker.add(number)
}

fun callSendBlock(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val block = createLambda()

    helper.sendBlock(block)
    helper.sendBlock(block)
    tracker.add(block)
}

private class EmptyContinuation : Continuation<Any?> {
    override val context: CoroutineContext = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

fun callSendCompletion(helper: NoAutoreleaseSendHelper, tracker: KotlinLivenessTracker) {
    val completion = EmptyContinuation()

    suspend {
        helper.sendCompletion()
        helper.sendCompletion()
    }.startCoroutine(completion)

    tracker.add(completion)
}

fun callReceiveKotlinObject(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveKotlinObject())
}

fun callReceiveSwiftObject(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveSwiftObject())
}

fun callReceiveList(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveList())
}

fun callReceiveString(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveString())
}

fun callReceiveNumber(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveNumber())
}

fun callReceiveBlock(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveBlock())
}

fun callReceiveBlockAndCall(helper: NoAutoreleaseReceiveHelper, tracker: KotlinLivenessTracker) = repeat(2) {
    tracker.add(helper.receiveBlock()())
}

fun objc_autoreleasePoolPush() = kotlinx.cinterop.objc_autoreleasePoolPush()
fun objc_autoreleasePoolPop(handle: NativePtr) = kotlinx.cinterop.objc_autoreleasePoolPop(handle)

fun useIntArray(array: IntArray) {} // Just to make IntArray available from Swift.

private fun createLambda(): () -> KotlinObject {
    val lambdaResult = KotlinObject()
    return { lambdaResult } // make it capturing thus dynamic.
}

private fun createLambda(kotlinLivenessTracker: KotlinLivenessTracker): () -> KotlinObject {
    val lambdaResult = KotlinObject()
    return {
        val result = lambdaResult // make it capturing thus dynamic.
        kotlinLivenessTracker.add(result)
        result
    }
}

private fun createKotlinNumber(): Any = (0.5 + Any().hashCode().toDouble()) // to make it dynamic.
