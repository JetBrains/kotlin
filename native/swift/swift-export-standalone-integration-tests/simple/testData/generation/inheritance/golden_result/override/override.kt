@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "8override4BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Sub::class, "8override3SubC")
@file:kotlin.native.internal.objc.BindClassToObjCName(P::class, "_P")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Base_g__TypesOfArguments__anyU20override_P__")
public fun Base_g__TypesOfArguments__anyU20override_P__(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as P
    __self.g(__x)
}

@ExportedBridge("P_f")
public fun P_f(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as P
    __self.f()
}

@ExportedBridge("Sub_g__TypesOfArguments__anyU20override_P__")
public fun Sub_g__TypesOfArguments__anyU20override_P__(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Sub
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as P
    __self.g(__x)
}

@ExportedBridge("__root___Base_init_allocate")
public fun __root___Base_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Base>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Base())
}

@ExportedBridge("__root___Sub_init_allocate")
public fun __root___Sub_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Sub>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Sub())
}
