@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(weird.A::class, "22ExportedKotlinPackages5weirdO13bad_overridesE1AC")
@file:kotlin.native.internal.objc.BindClassToObjCName(weird.B::class, "22ExportedKotlinPackages5weirdO13bad_overridesE1BC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("weird_A_bar_get")
public fun weird_A_bar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.A
    val _result = run { __self.bar }
    return _result
}

@ExportedBridge("weird_A_foo")
public fun weird_A_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.A
    val _result = run { __self.foo() }
    return run { _result; true }
}

@ExportedBridge("weird_A_init_allocate")
public fun weird_A_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<weird.A>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("weird_A_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun weird_A_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr, __error: kotlinx.cinterop.COpaquePointerVar): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val ____error = __error
    try {
        val _result = run { kotlin.native.internal.initInstance(____kt, weird.A()) }
        return run { _result; true }
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return false
    }
}

@ExportedBridge("weird_A_throws")
public fun weird_A_throws(self: kotlin.native.internal.NativePtr, _out_error: kotlinx.cinterop.COpaquePointerVar): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.A
    val ___out_error = _out_error
    try {
        val _result = run { __self.throws() }
        return run { _result; true }
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return false
    }
}

@ExportedBridge("weird_B_bar_get")
public fun weird_B_bar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.B
    val _result = run { __self.bar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("weird_B_foo")
public fun weird_B_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.B
    val _result = run { __self.foo() }
    return run { _result; true }
}

@ExportedBridge("weird_B_init_allocate")
public fun weird_B_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<weird.B>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("weird_B_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun weird_B_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, weird.B()) }
    return run { _result; true }
}

@ExportedBridge("weird_B_throws")
public fun weird_B_throws(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as weird.B
    val _result = run { __self.throws() }
    return run { _result; true }
}
