@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Outer::class, "4main5OuterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Outer.Inner::class, "4main5OuterC5InnerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Outer.Inner.InnerInner::class, "4main5OuterC5InnerC10InnerInnerC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Outer_Inner_InnerInner_init_allocate")
public fun Outer_Inner_InnerInner_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Outer.Inner.InnerInner>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Outer_Inner_InnerInner_init_initialize__TypesOfArguments__Swift_UInt_main_Outer_Inner__")
public fun Outer_Inner_InnerInner_init_initialize__TypesOfArguments__Swift_UInt_main_Outer_Inner__(__kt: kotlin.native.internal.NativePtr, outer__: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __outer__ = kotlin.native.internal.ref.dereferenceExternalRCRef(outer__) as Outer.Inner
    kotlin.native.internal.initInstance(____kt, (__outer__ as Outer.Inner).InnerInner())
}

@ExportedBridge("Outer_Inner_foo")
public fun Outer_Inner_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Outer.Inner
    val _result = __self.foo()
    return _result
}

@ExportedBridge("Outer_Inner_init_allocate")
public fun Outer_Inner_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Outer.Inner>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Outer_Inner_init_initialize__TypesOfArguments__Swift_UInt_main_Outer__")
public fun Outer_Inner_init_initialize__TypesOfArguments__Swift_UInt_main_Outer__(__kt: kotlin.native.internal.NativePtr, outer__: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __outer__ = kotlin.native.internal.ref.dereferenceExternalRCRef(outer__) as Outer
    kotlin.native.internal.initInstance(____kt, (__outer__ as Outer).Inner())
}

@ExportedBridge("__root___Outer_init_allocate")
public fun __root___Outer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Outer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Outer_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Outer_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Outer())
}

