@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.Flow::class, "_Flow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.SharedFlow::class, "_SharedFlow")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.coroutines.flow.StateFlow::class, "_StateFlow")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlinx_coroutines_flow_SharedFlow_replayCache_get")
public fun kotlinx_coroutines_flow_SharedFlow_replayCache_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.SharedFlow<kotlin.Any?>
    val _result = __self.replayCache
    return _result.objcPtr()
}

@ExportedBridge("kotlinx_coroutines_flow_StateFlow_value_get")
public fun kotlinx_coroutines_flow_StateFlow_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.coroutines.flow.StateFlow<kotlin.Any?>
    val _result = __self.value
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
