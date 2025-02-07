@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM_WITH_INTERFACE_INHERITANCE::class, "4main31ENUM_WITH_INTERFACE_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyObject::class, "4main8MyObjectC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_INTERFACE_INHERITANCE::class, "4main33OBJECT_WITH_INTERFACE_INHERITANCEC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Bar_bar__TypesOfArguments__Swift_Int32__")
public fun Bar_bar__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val __arg = arg
    val _result = __self.bar(__arg)
    return _result
}

@ExportedBridge("Bar_baz_get")
public fun Bar_baz_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = __self.baz
    return _result
}

@ExportedBridge("ENUM_WITH_INTERFACE_INHERITANCE_entries_get")
public fun ENUM_WITH_INTERFACE_INHERITANCE_entries_get(): kotlin.native.internal.NativePtr {
    val _result = ENUM_WITH_INTERFACE_INHERITANCE.entries
    return _result.objcPtr()
}

@ExportedBridge("ENUM_WITH_INTERFACE_INHERITANCE_valueOf__TypesOfArguments__Swift_String__")
public fun ENUM_WITH_INTERFACE_INHERITANCE_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = ENUM_WITH_INTERFACE_INHERITANCE.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foeble_bar__TypesOfArguments__Swift_Int32__")
public fun Foeble_bar__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foeble
    val __arg = arg
    val _result = __self.bar(__arg)
    return _result
}

@ExportedBridge("Foeble_baz_get")
public fun Foeble_baz_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foeble
    val _result = __self.baz
    return _result
}

@ExportedBridge("__root___Bar_init_allocate")
public fun __root___Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Bar_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Bar())
}

@ExportedBridge("__root___MyObject_get")
public fun __root___MyObject_get(): kotlin.native.internal.NativePtr {
    val _result = MyObject
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OBJECT_WITH_INTERFACE_INHERITANCE_get")
public fun __root___OBJECT_WITH_INTERFACE_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_WITH_INTERFACE_INHERITANCE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

