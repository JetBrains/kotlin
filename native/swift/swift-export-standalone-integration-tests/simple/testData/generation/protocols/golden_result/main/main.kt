@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM_WITH_INTERFACE_INHERITANCE::class, "4main31ENUM_WITH_INTERFACE_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyObject::class, "4main8MyObjectC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_INTERFACE_INHERITANCE::class, "4main33OBJECT_WITH_INTERFACE_INHERITANCEC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Bar_bar__TypesOfArguments__anyU20main_Foeble__")
public fun Bar_bar__TypesOfArguments__anyU20main_Foeble__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Foeble
    val _result = __self.bar(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Bar_baz_get")
public fun Bar_baz_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = __self.baz
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Barable_bar__TypesOfArguments__anyU20main_Foeble__")
public fun Barable_bar__TypesOfArguments__anyU20main_Foeble__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Barable
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Foeble
    val _result = __self.bar(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Barable_baz_get")
public fun Barable_baz_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Barable
    val _result = __self.baz
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("Foeble_bar__TypesOfArguments__anyU20main_Foeble__")
public fun Foeble_bar__TypesOfArguments__anyU20main_Foeble__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foeble
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Foeble
    val _result = __self.bar(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foeble_baz_get")
public fun Foeble_baz_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foeble
    val _result = __self.baz
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_bar__TypesOfArguments__anyU20main_Foeble__")
public fun Foo_bar__TypesOfArguments__anyU20main_Foeble__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Foeble
    val _result = __self.bar(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_baz_get")
public fun Foo_baz_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.baz
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Foo())
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

@ExportedBridge("__root___list__TypesOfArguments__Swift_Array_anyU20main_Foeble___")
public fun __root___list__TypesOfArguments__Swift_Array_anyU20main_Foeble___(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.collections.List<Foeble>>(value)
    val _result = list(__value)
    return _result.objcPtr()
}

@ExportedBridge("__root___list_get")
public fun __root___list_get(): kotlin.native.internal.NativePtr {
    val _result = list
    return _result.objcPtr()
}

@ExportedBridge("__root___list_set__TypesOfArguments__Swift_Array_anyU20main_Foeble___")
public fun __root___list_set__TypesOfArguments__Swift_Array_anyU20main_Foeble___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = interpretObjCPointer<kotlin.collections.List<Foeble>>(newValue)
    list = __newValue
}

@ExportedBridge("__root___normal__TypesOfArguments__anyU20main_Foeble__")
public fun __root___normal__TypesOfArguments__anyU20main_Foeble__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as Foeble
    val _result = normal(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normal_get")
public fun __root___normal_get(): kotlin.native.internal.NativePtr {
    val _result = normal
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normal_set__TypesOfArguments__anyU20main_Foeble__")
public fun __root___normal_set__TypesOfArguments__anyU20main_Foeble__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foeble
    normal = __newValue
}

@ExportedBridge("__root___nullable__TypesOfArguments__anyU20main_Foeble_opt___")
public fun __root___nullable__TypesOfArguments__anyU20main_Foeble_opt___(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as Foeble
    val _result = nullable(__value)
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullable_get")
public fun __root___nullable_get(): kotlin.native.internal.NativePtr {
    val _result = nullable
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullable_set__TypesOfArguments__anyU20main_Foeble_opt___")
public fun __root___nullable_set__TypesOfArguments__anyU20main_Foeble_opt___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foeble
    nullable = __newValue
}
