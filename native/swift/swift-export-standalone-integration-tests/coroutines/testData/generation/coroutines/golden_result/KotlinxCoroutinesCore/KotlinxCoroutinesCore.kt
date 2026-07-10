@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.Flow::class, "_Flow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.FlowCollector::class, "_FlowCollector")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.MutableSharedFlow::class, "_MutableSharedFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.MutableStateFlow::class, "_MutableStateFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.SharedFlow::class, "_SharedFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.StateFlow::class, "_StateFlow")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch as kotlinx_coroutines_launch

@ImportedBridge("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.FlowCollector::class, "emit")
public suspend fun kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlinx.coroutines.flow.FlowCollector<kotlin.Any?>, value: kotlin.Any?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    return suspendSwiftCoroutine { continuation: Function1<Unit, Unit>, exception: Function1<platform.Foundation.NSError?, Unit>, cancellation: SwiftJob ->
        val _continuation = kotlin.native.internal.ref.createRetainedExternalRCRef(continuation)
        val _exception = kotlin.native.internal.ref.createRetainedExternalRCRef(exception)
        val _cancellation = kotlin.native.internal.ref.createRetainedExternalRCRef(cancellation)
        kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __value, _continuation, _exception, _cancellation)
    }
}

@ImportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableSharedFlow::class, "emit")
public suspend fun kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>, value: kotlin.Any?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    return suspendSwiftCoroutine { continuation: Function1<Unit, Unit>, exception: Function1<platform.Foundation.NSError?, Unit>, cancellation: SwiftJob ->
        val _continuation = kotlin.native.internal.ref.createRetainedExternalRCRef(continuation)
        val _exception = kotlin.native.internal.ref.createRetainedExternalRCRef(exception)
        val _cancellation = kotlin.native.internal.ref.createRetainedExternalRCRef(cancellation)
        kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __value, _continuation, _exception, _cancellation)
    }
}

@ImportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableSharedFlow::class, "resetReplayCache")
public fun kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse(self: kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableSharedFlow::class, "<get-subscriptionCount>")
public fun kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse(self: kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>): kotlinx.coroutines.flow.StateFlow<Int> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlinx.coroutines.flow.StateFlow<Int>
}

@ImportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableSharedFlow::class, "tryEmit")
public fun kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>, value: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    val _result = kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __value)
    return _result
}

@ImportedBridge("kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, `expect`: kotlin.native.internal.NativePtr, update: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableStateFlow::class, "compareAndSet")
public fun kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>, `expect`: kotlin.Any?, update: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __expect = if (`expect` == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(`expect`)
    val __update = if (update == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(update)
    val _result = kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __expect, __update)
    return _result
}

@ImportedBridge("kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableStateFlow::class, "<get-value>")
public fun kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse(self: kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.MutableStateFlow::class, "<set-value>")
public fun kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlinx.coroutines.flow.MutableStateFlow<kotlin.Any?>, newValue: kotlin.Any?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = if (newValue == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(newValue)
    val _result = kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift")
internal external fun kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.SharedFlow::class, "<get-replayCache>")
public fun kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse(self: kotlinx.coroutines.flow.SharedFlow<kotlin.Any?>): kotlin.collections.List<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(_result)
}

@ImportedBridge("kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift")
internal external fun kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlinx.coroutines.flow.StateFlow::class, "<get-value>")
public fun kotlinx_coroutines_flow_StateFlow_value_get__reverse(self: kotlinx.coroutines.flow.StateFlow<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ExportedBridge("KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
public fun KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Boolean): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run<Unit> { _1 }
    val _result = run { (__pointerToBlock as Function1<Unit, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___")
public fun KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = if (_1 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<platform.Foundation.NSError>(_1)
    val _result = run { (__pointerToBlock as Function1<platform.Foundation.NSError?, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__")
public fun kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Boolean>(function);
        suspend { arg0: kotlin.Any? ->
            suspendSwiftCoroutine { continuation: Function1<Unit, Unit>, exception: Function1<platform.Foundation.NSError?, Unit>, cancellation: SwiftJob ->
                val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
                val _continuation = kotlin.native.internal.ref.createRetainedExternalRCRef(continuation)
                val _exception = kotlin.native.internal.ref.createRetainedExternalRCRef(exception)
                val _cancellation = kotlin.native.internal.ref.createRetainedExternalRCRef(cancellation)
                val _result = kotlinFun(_arg0, _continuation, _exception, _cancellation)
                run<Unit> { _result }
            }
        }
    }
    val _result = run { kotlinx.coroutines.flow.FlowCollector<kotlin.Any?>(__function) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.FlowCollector<kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->Boolean>(continuation);
        { arg0: Unit ->
            val _arg0 = run { arg0; true }
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.emit(__value)
    }
}

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.MutableSharedFlow<kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->Boolean>(continuation);
        { arg0: Unit ->
            val _arg0 = run { arg0; true }
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.emit(__value)
    }
}

@ExportedBridge("kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
