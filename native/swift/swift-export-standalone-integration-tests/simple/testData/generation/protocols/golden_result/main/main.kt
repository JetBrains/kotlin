@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Child1::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE6Child1C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Child2::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE6Child2C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Child3::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE6Child3C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Child4::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE6Child4C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Child5::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE6Child5C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.GrandChild1::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE11GrandChild1C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.GrandChild2::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE11GrandChild2C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.GrandChild3::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE11GrandChild3C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.GrandChild4::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE11GrandChild4C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.GrandChild5::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE11GrandChild5C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Parent1::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE7Parent1C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Parent2::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE7Parent2C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Parent3::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE7Parent3C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Parent4::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE7Parent4C")
@file:kotlin.native.internal.objc.BindClassToObjCName(repeating_conformances.Parent5::class, "22ExportedKotlinPackages22repeating_conformancesO4mainE7Parent5C")
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

@ExportedBridge("__root___nullable__TypesOfArguments__Swift_Optional_anyU20main_Foeble___")
public fun __root___nullable__TypesOfArguments__Swift_Optional_anyU20main_Foeble___(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as Foeble
    val _result = nullable(__value)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullable_get")
public fun __root___nullable_get(): kotlin.native.internal.NativePtr {
    val _result = nullable
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullable_set__TypesOfArguments__Swift_Optional_anyU20main_Foeble___")
public fun __root___nullable_set__TypesOfArguments__Swift_Optional_anyU20main_Foeble___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foeble
    nullable = __newValue
}

@ExportedBridge("repeating_conformances_Child1_init_allocate")
public fun repeating_conformances_Child1_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Child1>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Child1_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Child1_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Child1())
}

@ExportedBridge("repeating_conformances_Child2_init_allocate")
public fun repeating_conformances_Child2_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Child2>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Child2_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Child2_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Child2())
}

@ExportedBridge("repeating_conformances_Child3_init_allocate")
public fun repeating_conformances_Child3_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Child3>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Child3_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Child3_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Child3())
}

@ExportedBridge("repeating_conformances_Child4_init_allocate")
public fun repeating_conformances_Child4_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Child4>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Child4_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Child4_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Child4())
}

@ExportedBridge("repeating_conformances_Child5_init_allocate")
public fun repeating_conformances_Child5_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Child5>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Child5_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Child5_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Child5())
}

@ExportedBridge("repeating_conformances_GrandChild1_init_allocate")
public fun repeating_conformances_GrandChild1_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.GrandChild1>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_GrandChild1_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_GrandChild1_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.GrandChild1())
}

@ExportedBridge("repeating_conformances_GrandChild2_init_allocate")
public fun repeating_conformances_GrandChild2_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.GrandChild2>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_GrandChild2_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_GrandChild2_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.GrandChild2())
}

@ExportedBridge("repeating_conformances_GrandChild3_init_allocate")
public fun repeating_conformances_GrandChild3_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.GrandChild3>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_GrandChild3_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_GrandChild3_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.GrandChild3())
}

@ExportedBridge("repeating_conformances_GrandChild4_init_allocate")
public fun repeating_conformances_GrandChild4_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.GrandChild4>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_GrandChild4_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_GrandChild4_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.GrandChild4())
}

@ExportedBridge("repeating_conformances_GrandChild5_init_allocate")
public fun repeating_conformances_GrandChild5_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.GrandChild5>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_GrandChild5_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_GrandChild5_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.GrandChild5())
}

@ExportedBridge("repeating_conformances_Parent1_init_allocate")
public fun repeating_conformances_Parent1_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Parent1>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Parent1_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Parent1_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Parent1())
}

@ExportedBridge("repeating_conformances_Parent2_init_allocate")
public fun repeating_conformances_Parent2_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Parent2>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Parent2_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Parent2_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Parent2())
}

@ExportedBridge("repeating_conformances_Parent3_init_allocate")
public fun repeating_conformances_Parent3_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Parent3>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Parent3_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Parent3_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Parent3())
}

@ExportedBridge("repeating_conformances_Parent4_init_allocate")
public fun repeating_conformances_Parent4_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Parent4>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Parent4_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Parent4_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Parent4())
}

@ExportedBridge("repeating_conformances_Parent5_init_allocate")
public fun repeating_conformances_Parent5_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<repeating_conformances.Parent5>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("repeating_conformances_Parent5_init_initialize__TypesOfArguments__Swift_UInt__")
public fun repeating_conformances_Parent5_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, repeating_conformances.Parent5())
}
