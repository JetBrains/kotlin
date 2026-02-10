@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(SwiftJob::class, "KotlinTask")
@file:kotlin.native.internal.objc.BindClassToObjCName(SwiftFlowIterator::class, "KotlinFlowIterator")

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.concurrent.atomics.AtomicReference
import kotlin.native.internal.ExportedBridge
import kotlin.plus

@OptIn(InternalCoroutinesApi::class)
fun Job.alsoCancel(another: Job) {
    // It is necessary to forward cancellation as soon as it is triggered to make it visible before the job completes,
    // hence onCancelling=true
    this.invokeOnCompletion(onCancelling = true) {
        if (it is CancellationException) {
            another.cancel()
        }
    }
}

/**
 * A Swift.Task-based Job.
 *
 * This type is a manually bridged counterpart to KotlinTask type in Swift
 *
 * @property backingJob the job this class delegates Job conformance to.
 * @property cancellationCallback callback used to control or query the external cancellation state.
 *  Accepts Boolean that indicates if the external work shall be cancelled.
 *  Returns Boolean indicating if the external work was already cancelled before the call.
 *
 */
@OptIn(InternalCoroutinesApi::class)
class SwiftJob private constructor(
    val backingJob: Job,
    val cancellationCallback: (Boolean) -> Boolean,
) : Job by backingJob {
    constructor(cancellationCallback: (Boolean) -> Boolean = { it }) : this(backingJob = Job(), cancellationCallback = cancellationCallback)

    init {
        // It is necessary to forward cancellation as soon as it is triggered to make it visible before the job completes,
        // hence onCancelling=true
        backingJob.invokeOnCompletion(onCancelling = true) {
            if (it is CancellationException) {
                cancellationCallback(true)
            }
        }
        if (cancellationCallback(false)) {
            backingJob.cancel()
        }
    }

    fun cancelExternally() {
        backingJob.cancel(CancellationException("Hosting Swift.Task was cancelled externally."))
    }
}

@ExportedBridge("__root___SwiftJob_init_allocate")
public fun __root___SwiftJob_init_allocate(): kotlin.native.internal.NativePtr {
    val instance = kotlin.native.internal.createUninitializedInstance<SwiftJob>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(instance)
}

@ExportedBridge("__root___SwiftJob_init_initialize")
public fun __root___SwiftJob_init_initialize(__kt: kotlin.native.internal.NativePtr, _block: kotlin.native.internal.NativePtr): Unit {
    val instance = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val block = convertBlockPtrToKotlinFunction<(Boolean)->Boolean>(_block)
    kotlin.native.internal.initInstance(instance, SwiftJob(cancellationCallback = block))
}

@ExportedBridge("__root___SwiftJob_cancelExternally")
public fun __root___SwiftJob_cancelExternally(self: kotlin.native.internal.NativePtr): Unit {
    val instance = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SwiftJob
    instance.cancelExternally()
}

/**
 * A pull-based iterator for kotlinx.coroutines.Flow that is interface-compatible with Swift.AsyncIteratorProtocol.
 *
 * This type is a manually bridged counterpart to KotlinFlowIterator type in Swift
 *
 */

@OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
class SwiftFlowIterator<T> private constructor(
    private val state: AtomicReference<SwiftFlowIterator.State>,
): FlowCollector<T> {
    private object Retry
    private class Throw(val exception: Throwable)

    private sealed interface State {
        // State transitions:
        //
        // Ready -> AwaitingProducer [label=next];
        // Ready -> Completed [label=complete];
        //
        // AwaitingConsumer -> AwaitingProducer [label=next];
        // AwaitingConsumer -> Completed [label=complete];
        //
        // AwaitingProducer -> AwaitingConsumer [label=emit];
        // AwaitingProducer -> Completed [label=complete];

        data class Ready<T>(val flow: Flow<T>) : State
        data class AwaitingConsumer(val continuation: CancellableContinuation<Unit>) : State
        data class AwaitingProducer(val continuation: CancellableContinuation<Any?>) : State
        data class Completed(val error: Throwable?) : State
    }

    public constructor(flow: Flow<T>) : this(state = AtomicReference(State.Ready<T>(flow)))

    public fun cancel() = complete(CancellationException("Flow cancelled"))

    @Suppress("UNCHECKED_CAST")
    public fun complete(error: Throwable?) {
        loop@ while (true) {
            when (val state = this@SwiftFlowIterator.state.exchange(State.Completed(error))) {
                is State.Ready<*> -> return
                is State.AwaitingProducer -> if (error != null) {
                    state.continuation.resumeWithException(error)
                } else {
                    state.continuation.resume(null)
                }
                is State.AwaitingConsumer -> if (error != null) {
                    state.continuation.resumeWithException(error)
                } else {
                    error("prematurely completing flow collection without an error")
                }
                is State.Completed -> break
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public override suspend fun emit(value: T) {
        loop@while (true) {
            when (val state = this@SwiftFlowIterator.state.load()) {
                is State.Ready<*> -> {
                    error("Internal inconsistency: flow collection was started prematurely")
                }
                is State.AwaitingProducer -> {
                    return suspendCancellableCoroutine<Any> { continuation ->
                        val newState = State.AwaitingConsumer(continuation)
                        if (!this@SwiftFlowIterator.state.compareAndSet(state, newState)) {
                            continuation.resume(Retry) // state changed; continue the outer loop
                        } else {
                            state.continuation.resume(value) // continue the consumer
                        }
                    }.handleActions<Unit> { continue@loop }
                }
                is State.AwaitingConsumer -> {
                    error("KotlinFlowIterator doesn't support concurrent iteration")
                }
                is State.Completed -> throw state.error ?: error("Emitting into already comleted stream.")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    public suspend fun next(): T? {
        loop@while (true) {
            when (val state = this@SwiftFlowIterator.state.load()) {
                is State.Ready<*> -> {
                    val state = state as State.Ready<T>
                    return suspendCancellableCoroutine<Any?> { continuation ->
                        val newState = State.AwaitingProducer(continuation)
                        if (!this@SwiftFlowIterator.state.compareAndSet(state, newState)) {
                            continuation.resume(Retry) // state changed; continue the outer loop
                        } else {
                            this.launch(state.flow)
                        }
                    }?.handleActions<T> { continue@loop }
                }
                is State.AwaitingConsumer -> {
                    return suspendCancellableCoroutine<Any?> { continuation ->
                        val newState = State.AwaitingProducer(continuation)
                        if (!this@SwiftFlowIterator.state.compareAndSet(state, newState)) {
                            continuation.resume(Retry) // state changed; continue the outer loop
                        } else {
                            state.continuation.resume(Unit) // continue the producer
                        }
                    }?.handleActions<T> { continue@loop }
                }
                is State.AwaitingProducer -> {
                    error("KotlinFlowIterator doesn't support concurrent receivers")
                }
                is State.Completed -> return state.error?.let { throw it } ?: null
            }
        }
    }

    private fun launch(flow: Flow<T>) {
        CoroutineScope(EmptyCoroutineContext).launch {
            flow
                .catch { complete(it) }
                .collect(this@SwiftFlowIterator)
        }.invokeOnCompletion {
            complete(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun<T> Any.handleActions(onRetry: () -> T): T {
        return when (this) {
            is Retry -> onRetry()
            is Throw -> throw exception
            else -> this as T
        }
    }
}

@ExportedBridge("_kotlin_swift_SwiftFlowIterator_cancel")
public fun SwiftFlowIterator_cancel(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRefOrNull(self) as SwiftFlowIterator<kotlin.Any?>?
    __self?.cancel()
}

@ExportedBridge("_kotlin_swift_SwiftFlowIterator_next")
public fun SwiftFlowIterator_next(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SwiftFlowIterator<kotlin.Any?>
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(continuation);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = __self.next()
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("_kotlin_swift_SwiftFlowIterator_init_allocate")
public fun __root___SwiftFlowIterator_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<SwiftFlowIterator<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("_kotlin_swift_SwiftFlowIterator_init_initialize")
public fun __root___SwiftFlowIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_Flow__(__kt: kotlin.native.internal.NativePtr, flow: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __flow = kotlin.native.internal.ref.dereferenceExternalRCRef(flow) as kotlinx.coroutines.flow.Flow<kotlin.Any?>
    kotlin.native.internal.initInstance(____kt, SwiftFlowIterator<kotlin.Any?>(__flow))
}