@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_any_any_bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun generation_any_any_bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = generation.any.any.bar(__arg)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_any_any_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun generation_any_any_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = generation.any.any.foo(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
