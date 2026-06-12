@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "8override4BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Sub::class, "8override3SubC")
@file:kotlin.native.internal.objc.BindClassToObjCName(P::class, "_P")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("Base_g__TypesOfArguments__anyU20override_P____reverse_swift")
internal external fun Base_g__TypesOfArguments__anyU20override_P____reverse_swift(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Base::class, "g")
public fun Base_g__TypesOfArguments__anyU20override_P____reverse(self: Base, x: P): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __x = kotlin.native.internal.ref.createRetainedExternalRCRef(x)
    val __result = Base_g__TypesOfArguments__anyU20override_P____reverse_swift(__self, __x)
    return run<Unit> { __result }
}

@ImportedBridge("P_f__reverse_swift")
internal external fun P_f__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(P::class, "f")
public fun P_f__reverse(self: P): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = P_f__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Sub_g__TypesOfArguments__anyU20override_P____reverse_swift")
internal external fun Sub_g__TypesOfArguments__anyU20override_P____reverse_swift(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Sub::class, "g")
public fun Sub_g__TypesOfArguments__anyU20override_P____reverse(self: Sub, x: P): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __x = kotlin.native.internal.ref.createRetainedExternalRCRef(x)
    val __result = Sub_g__TypesOfArguments__anyU20override_P____reverse_swift(__self, __x)
    return run<Unit> { __result }
}

@ExportedBridge("Base_g__TypesOfArguments__anyU20override_P__")
public fun Base_g__TypesOfArguments__anyU20override_P__(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as P
    val _result = run { __self.g(__x) }
    return run { _result; true }
}

@ExportedBridge("P_f")
public fun P_f(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as P
    val _result = run { __self.f() }
    return run { _result; true }
}

@ExportedBridge("Sub_g__TypesOfArguments__anyU20override_P__")
public fun Sub_g__TypesOfArguments__anyU20override_P__(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Sub
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as P
    val _result = run { __self.g(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___Base_init_allocate")
public fun __root___Base_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Base>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Base()) }
    return run { _result; true }
}

@ExportedBridge("__root___Sub_init_allocate")
public fun __root___Sub_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Sub>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Sub_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Sub()) }
    return run { _result; true }
}
