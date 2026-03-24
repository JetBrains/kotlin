@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.Flow::class, "_Flow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.FlowCollector::class, "_FlowCollector")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.MutableSharedFlow::class, "_MutableSharedFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.MutableStateFlow::class, "_MutableStateFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.SharedFlow::class, "_SharedFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.StateFlow::class, "_StateFlow")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.*

@ExportedBridge("KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
public fun KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Boolean): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run<Unit> { _1 }
    val _result = run { (__pointerToBlock as Function1<Unit, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__")
public fun KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = interpretObjCPointer<platform.Foundation.NSError>(_1)
    val _result = run { (__pointerToBlock as Function1<platform.Foundation.NSError, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__")
public fun kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val originalBlock = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Unit>(function);
        suspend { arg0: kotlin.Any? ->
            val __cancellation: SwiftJob = SwiftJob()
            kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.let {
                __cancellation.alsoCancel(it)
                it.alsoCancel(__cancellation)
            }

            kotlinx.coroutines.suspendCancellableCoroutine { __cont ->
                val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
                val __continuation: Function1<Unit, Unit> = { _result ->
                    __cont.resumeWith(kotlin.Result.success(_result))
                }
                val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
                val __exception: Function1<platform.Foundation.NSError, Unit> = { _error ->
                    __cont.resumeWith(kotlin.Result.failure(SwiftException(_error)))
                }
                val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
                originalBlock(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0), __continuationPtr, __exceptionPtr, __cancellationPtr)
            }
        }
    }
    val _result = run { kotlinx.coroutines.flow.FlowCollector(__function) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.FlowCollector<kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->Boolean>(continuation);
        { arg0: Unit ->
            val _result = kotlinFun(run { arg0; true })
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = __self.emit(__value)
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

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->Boolean>(continuation);
        { arg0: Unit ->
            val _result = kotlinFun(run { arg0; true })
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = __self.emit(__value)
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

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache")
public fun kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>
    val _result = run { __self.resetReplayCache() }
    return run { _result; true }
}

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get")
public fun kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>
    val _result = run { __self.subscriptionCount }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { __self.tryEmit(__value) }
    return _result
}

@ExportedBridge("kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, `expect`: kotlin.native.internal.NativePtr, update: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>
    val __expect = if (`expect` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`expect`) as kotlin.Any
    val __update = if (update == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(update) as kotlin.Any
    val _result = run { __self.compareAndSet(__expect, __update) }
    return _result
}

@ExportedBridge("kotlinx_coroutines_flow_MutableStateFlow_value_get")
public fun kotlinx_coroutines_flow_MutableStateFlow_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    val _result = run { __self.value = __newValue }
    return run { _result; true }
}

@ExportedBridge("kotlinx_coroutines_flow_SharedFlow_replayCache_get")
public fun kotlinx_coroutines_flow_SharedFlow_replayCache_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.SharedFlow<kotlin.Any?>
    val _result = run { __self.replayCache }
    return _result.objcPtr()
}

@ExportedBridge("kotlinx_coroutines_flow_StateFlow_value_get")
public fun kotlinx_coroutines_flow_StateFlow_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.StateFlow<kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
