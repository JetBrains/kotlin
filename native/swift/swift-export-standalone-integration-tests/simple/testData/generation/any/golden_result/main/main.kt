@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = bar(__arg)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = foo(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
