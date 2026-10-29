@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Derived::class, "4main7DerivedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Derived_checked")
public fun Derived_checked(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Derived
    val _result = __self.checked()
    return _result
}

@ExportedBridge("Derived_ignorable")
public fun Derived_ignorable(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Derived
    val _result = __self.ignorable()
    return _result
}

@ExportedBridge("Foo_ignorableMember")
public fun Foo_ignorableMember(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.ignorableMember()
    return _result
}

@ExportedBridge("Foo_member")
public fun Foo_member(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.member()
    return _result
}

@ExportedBridge("Foo_property_get")
public fun Foo_property_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.`property`
    return _result
}

@ExportedBridge("__root___Derived_init_allocate")
public fun __root___Derived_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Derived>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Derived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Derived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Derived())
    return true
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Foo())
    return true
}

@ExportedBridge("__root___function")
public fun __root___function(): Int {
    val _result = function()
    return _result
}

@ExportedBridge("__root___ignorable")
public fun __root___ignorable(): Int {
    val _result = ignorable()
    return _result
}

@ExportedBridge("__root___nothingFunction")
public fun __root___nothingFunction(): Boolean {
    val _result = nothingFunction()
    return _result
}

@ExportedBridge("__root___unitFunction")
public fun __root___unitFunction(): Boolean {
    unitFunction()
    return true
}
