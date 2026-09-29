@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(RegularKotlinClass::class, "4main18RegularKotlinClassC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___RegularKotlinClass_init_allocate")
public fun __root___RegularKotlinClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<RegularKotlinClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___RegularKotlinClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___RegularKotlinClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, RegularKotlinClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___consumeCopyable__TypesOfArguments__anyU20Foundation_NSCopying__")
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
public fun __root___consumeCopyable__TypesOfArguments__anyU20Foundation_NSCopying__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = interpretObjCPointer<platform.Foundation.NSCopyingProtocol>(x)
    val _result = run { consumeCopyable(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___produceCopyable")
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
public fun __root___produceCopyable(): kotlin.native.internal.NativePtr {
    val _result = run { produceCopyable() }
    return _result.objcPtr()
}

@ExportedBridge("__root___produceZar")
@OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
public fun __root___produceZar(): kotlin.native.internal.NativePtr {
    val _result = run { produceZar() }
    return _result.objcPtr()
}
