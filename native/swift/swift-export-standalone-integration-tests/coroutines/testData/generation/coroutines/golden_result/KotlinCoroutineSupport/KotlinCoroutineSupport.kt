@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(SwiftJob::class, "KotlinTask")

import kotlinx.coroutines.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.native.internal.ExportedBridge

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
