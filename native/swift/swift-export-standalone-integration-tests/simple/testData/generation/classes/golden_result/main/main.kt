@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Foo::class, "22ExportedKotlinPackages9namespaceO4mainE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Foo.INSIDE_CLASS::class, "22ExportedKotlinPackages9namespaceO4mainE3FooC12INSIDE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.NAMESPACED_CLASS::class, "22ExportedKotlinPackages9namespaceO4mainE16NAMESPACED_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.DATA_OBJECT_WITH_PACKAGE::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE24DATA_OBJECT_WITH_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Foo::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Foo.INSIDE_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE3FooC12INSIDE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE3FooC12INSIDE_CLASSC19DEEPER_INSIDE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.NAMESPACED_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE16NAMESPACED_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.OBJECT_WITH_PACKAGE::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19OBJECT_WITH_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.OBJECT_WITH_PACKAGE.Bar::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19OBJECT_WITH_PACKAGEC3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19OBJECT_WITH_PACKAGEC3BarC19OBJECT_INSIDE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.OBJECT_WITH_PACKAGE.Foo::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19OBJECT_WITH_PACKAGEC3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19OBJECT_WITH_PACKAGEC20OBJECT_INSIDE_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(why_we_need_module_names.CLASS_WITH_SAME_NAME::class, "22ExportedKotlinPackages24why_we_need_module_namesO4mainE20CLASS_WITH_SAME_NAMEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ABSTRACT_CLASS::class, "4main14ABSTRACT_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(CLASS_WITH_SAME_NAME::class, "4main20CLASS_WITH_SAME_NAMEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassWithNonPublicConstructor::class, "4main29ClassWithNonPublicConstructorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS::class, "4main10DATA_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS_WITH_MANY_FIELDS::class, "4main27DATA_CLASS_WITH_MANY_FIELDSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS_WITH_REF::class, "4main19DATA_CLASS_WITH_REFC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM::class, "4main4ENUMC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM.INSIDE_ENUM::class, "4main4ENUMC11INSIDE_ENUMC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo.Companion::class, "4main3FooC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo.INSIDE_CLASS::class, "4main3FooC12INSIDE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE::class, "4main17OBJECT_NO_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE.Bar::class, "4main17OBJECT_NO_PACKAGEC3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE.Bar.CLASS_INSIDE_CLASS_INSIDE_OBJECT::class, "4main17OBJECT_NO_PACKAGEC3BarC32CLASS_INSIDE_CLASS_INSIDE_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE.Bar.NamedCompanion::class, "4main17OBJECT_NO_PACKAGEC3BarC14NamedCompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE.Foo::class, "4main17OBJECT_NO_PACKAGEC3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT::class, "4main17OBJECT_NO_PACKAGEC20OBJECT_INSIDE_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED::class, "4main6SEALEDC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED.C::class, "4main6SEALEDC1CC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED.O::class, "4main6SEALEDC1OC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("CLASS_WITH_SAME_NAME_foo")
public fun CLASS_WITH_SAME_NAME_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as CLASS_WITH_SAME_NAME
    val _result = __self.foo()
    return _result
}

@ExportedBridge("ClassWithNonPublicConstructor_a_get")
public fun ClassWithNonPublicConstructor_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassWithNonPublicConstructor
    val _result = __self.a
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_a_get")
public fun DATA_CLASS_WITH_MANY_FIELDS_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.a
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_b_get")
public fun DATA_CLASS_WITH_MANY_FIELDS_b_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.b
    return _result.objcPtr()
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_c_get")
public fun DATA_CLASS_WITH_MANY_FIELDS_c_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.c
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_copy__TypesOfArguments__Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__")
public fun DATA_CLASS_WITH_MANY_FIELDS_copy__TypesOfArguments__Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__(self: kotlin.native.internal.NativePtr, a: Int, b: kotlin.native.internal.NativePtr, c: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val __a = a
    val __b = interpretObjCPointer<kotlin.String>(b)
    val __c = kotlin.native.internal.ref.dereferenceExternalRCRef(c) as kotlin.Any
    val _result = __self.copy(__a, __b, __c)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_d_get")
public fun DATA_CLASS_WITH_MANY_FIELDS_d_get(self: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.d
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_e_get")
public fun DATA_CLASS_WITH_MANY_FIELDS_e_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.e
    return _result.objcPtr()
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_hashCode")
public fun DATA_CLASS_WITH_MANY_FIELDS_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_MANY_FIELDS_toString")
public fun DATA_CLASS_WITH_MANY_FIELDS_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_MANY_FIELDS
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("DATA_CLASS_WITH_REF_copy__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun DATA_CLASS_WITH_REF_copy__TypesOfArguments__KotlinRuntime_KotlinBase__(self: kotlin.native.internal.NativePtr, o: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val __o = kotlin.native.internal.ref.dereferenceExternalRCRef(o) as kotlin.Any
    val _result = __self.copy(__o)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_REF_hashCode")
public fun DATA_CLASS_WITH_REF_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_REF_o_get")
public fun DATA_CLASS_WITH_REF_o_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = __self.o
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_REF_toString")
public fun DATA_CLASS_WITH_REF_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("DATA_CLASS_a_get")
public fun DATA_CLASS_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = __self.a
    return _result
}

@ExportedBridge("DATA_CLASS_copy__TypesOfArguments__Swift_Int32__")
public fun DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, a: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val __a = a
    val _result = __self.copy(__a)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_hashCode")
public fun DATA_CLASS_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("DATA_CLASS_toString")
public fun DATA_CLASS_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("ENUM_A_get")
public fun ENUM_A_get(): kotlin.native.internal.NativePtr {
    val _result = ENUM.A
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_B_get")
public fun ENUM_B_get(): kotlin.native.internal.NativePtr {
    val _result = ENUM.B
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_C_get")
public fun ENUM_C_get(): kotlin.native.internal.NativePtr {
    val _result = ENUM.C
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_INSIDE_ENUM_init_allocate")
public fun ENUM_INSIDE_ENUM_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ENUM.INSIDE_ENUM>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UInt__")
public fun ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ENUM.INSIDE_ENUM())
}

@ExportedBridge("ENUM_entries_get")
public fun ENUM_entries_get(): kotlin.native.internal.NativePtr {
    val _result = ENUM.entries
    return _result.objcPtr()
}

@ExportedBridge("ENUM_valueOf__TypesOfArguments__Swift_String__")
public fun ENUM_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = ENUM.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_Companion_get")
public fun Foo_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = Foo.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_Companion_my_func")
public fun Foo_Companion_my_func(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.Companion
    val _result = __self.my_func()
    return _result
}

@ExportedBridge("Foo_Companion_my_value_inner_get")
public fun Foo_Companion_my_value_inner_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.Companion
    val _result = __self.my_value_inner
    return _result
}

@ExportedBridge("Foo_Companion_my_variable_inner_get")
public fun Foo_Companion_my_variable_inner_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.Companion
    val _result = __self.my_variable_inner
    return _result
}

@ExportedBridge("Foo_Companion_my_variable_inner_set__TypesOfArguments__Swift_Int64__")
public fun Foo_Companion_my_variable_inner_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.Companion
    val __newValue = newValue
    __self.my_variable_inner = __newValue
}

@ExportedBridge("Foo_INSIDE_CLASS_init_allocate")
public fun Foo_INSIDE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo.INSIDE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Foo.INSIDE_CLASS())
}

@ExportedBridge("Foo_INSIDE_CLASS_my_func")
public fun Foo_INSIDE_CLASS_my_func(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.INSIDE_CLASS
    val _result = __self.my_func()
    return _result
}

@ExportedBridge("Foo_INSIDE_CLASS_my_value_inner_get")
public fun Foo_INSIDE_CLASS_my_value_inner_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.INSIDE_CLASS
    val _result = __self.my_value_inner
    return _result
}

@ExportedBridge("Foo_INSIDE_CLASS_my_variable_inner_get")
public fun Foo_INSIDE_CLASS_my_variable_inner_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.INSIDE_CLASS
    val _result = __self.my_variable_inner
    return _result
}

@ExportedBridge("Foo_INSIDE_CLASS_my_variable_inner_set__TypesOfArguments__Swift_Int64__")
public fun Foo_INSIDE_CLASS_my_variable_inner_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo.INSIDE_CLASS
    val __newValue = newValue
    __self.my_variable_inner = __newValue
}

@ExportedBridge("Foo_foo")
public fun Foo_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.foo()
    return _result
}

@ExportedBridge("Foo_my_value_get")
public fun Foo_my_value_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.my_value
    return _result
}

@ExportedBridge("Foo_my_variable_get")
public fun Foo_my_variable_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.my_variable
    return _result
}

@ExportedBridge("Foo_my_variable_set__TypesOfArguments__Swift_Int64__")
public fun Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate")
public fun OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OBJECT_NO_PACKAGE.Bar.CLASS_INSIDE_CLASS_INSIDE_OBJECT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OBJECT_NO_PACKAGE.Bar.CLASS_INSIDE_CLASS_INSIDE_OBJECT())
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_NamedCompanion_foo")
public fun OBJECT_NO_PACKAGE_Bar_NamedCompanion_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE.Bar.NamedCompanion
    val _result = __self.foo()
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_NamedCompanion_get")
public fun OBJECT_NO_PACKAGE_Bar_NamedCompanion_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_NO_PACKAGE.Bar.NamedCompanion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_bar")
public fun OBJECT_NO_PACKAGE_Bar_bar(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE.Bar
    val _result = __self.bar()
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_i_get")
public fun OBJECT_NO_PACKAGE_Bar_i_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE.Bar
    val _result = __self.i
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_init_allocate")
public fun OBJECT_NO_PACKAGE_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OBJECT_NO_PACKAGE.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun OBJECT_NO_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, i: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __i = i
    kotlin.native.internal.initInstance(____kt, OBJECT_NO_PACKAGE.Bar(__i))
}

@ExportedBridge("OBJECT_NO_PACKAGE_Foo_init_allocate")
public fun OBJECT_NO_PACKAGE_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OBJECT_NO_PACKAGE.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun OBJECT_NO_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OBJECT_NO_PACKAGE.Foo())
}

@ExportedBridge("OBJECT_NO_PACKAGE_OBJECT_INSIDE_OBJECT_get")
public fun OBJECT_NO_PACKAGE_OBJECT_INSIDE_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_foo")
public fun OBJECT_NO_PACKAGE_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE
    val _result = __self.foo()
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_value_get")
public fun OBJECT_NO_PACKAGE_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE
    val _result = __self.value
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_variable_get")
public fun OBJECT_NO_PACKAGE_variable_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE
    val _result = __self.variable
    return _result
}

@ExportedBridge("OBJECT_NO_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__")
public fun OBJECT_NO_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE
    val __newValue = newValue
    __self.variable = __newValue
}

@ExportedBridge("SEALED_C_init_allocate")
public fun SEALED_C_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<SEALED.C>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("SEALED_C_init_initialize__TypesOfArguments__Swift_UInt__")
public fun SEALED_C_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, SEALED.C())
}

@ExportedBridge("SEALED_O_get")
public fun SEALED_O_get(): kotlin.native.internal.NativePtr {
    val _result = SEALED.O
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CLASS_WITH_SAME_NAME_init_allocate")
public fun __root___CLASS_WITH_SAME_NAME_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<CLASS_WITH_SAME_NAME>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, CLASS_WITH_SAME_NAME())
}

@ExportedBridge("__root___DATA_CLASS_WITH_MANY_FIELDS_init_allocate")
public fun __root___DATA_CLASS_WITH_MANY_FIELDS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DATA_CLASS_WITH_MANY_FIELDS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_WITH_MANY_FIELDS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__")
public fun __root___DATA_CLASS_WITH_MANY_FIELDS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__(__kt: kotlin.native.internal.NativePtr, a: Int, b: kotlin.native.internal.NativePtr, c: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    val __b = interpretObjCPointer<kotlin.String>(b)
    val __c = kotlin.native.internal.ref.dereferenceExternalRCRef(c) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, DATA_CLASS_WITH_MANY_FIELDS(__a, __b, __c))
}

@ExportedBridge("__root___DATA_CLASS_WITH_REF_init_allocate")
public fun __root___DATA_CLASS_WITH_REF_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DATA_CLASS_WITH_REF>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__")
public fun __root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__(__kt: kotlin.native.internal.NativePtr, o: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __o = kotlin.native.internal.ref.dereferenceExternalRCRef(o) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, DATA_CLASS_WITH_REF(__o))
}

@ExportedBridge("__root___DATA_CLASS_init_allocate")
public fun __root___DATA_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DATA_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, a: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    kotlin.native.internal.initInstance(____kt, DATA_CLASS(__a))
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, a: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    kotlin.native.internal.initInstance(____kt, Foo(__a))
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Float__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Float__(__kt: kotlin.native.internal.NativePtr, f: Float): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __f = f
    kotlin.native.internal.initInstance(____kt, Foo(__f))
}

@ExportedBridge("__root___OBJECT_NO_PACKAGE_get")
public fun __root___OBJECT_NO_PACKAGE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_NO_PACKAGE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_INSIDE_CLASS_init_allocate")
public fun namespace_Foo_INSIDE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Foo.INSIDE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Foo.INSIDE_CLASS())
}

@ExportedBridge("namespace_Foo_foo")
public fun namespace_Foo_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_Foo_init_allocate")
public fun namespace_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Foo())
}

@ExportedBridge("namespace_Foo_my_value_get")
public fun namespace_Foo_my_value_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = __self.my_value
    return _result
}

@ExportedBridge("namespace_Foo_my_variable_get")
public fun namespace_Foo_my_variable_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = __self.my_variable
    return _result
}

@ExportedBridge("namespace_Foo_my_variable_set__TypesOfArguments__Swift_Int64__")
public fun namespace_Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_NAMESPACED_CLASS_init_allocate")
public fun namespace_NAMESPACED_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.NAMESPACED_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.NAMESPACED_CLASS())
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_foo")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_get")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_hashCode")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_toString")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_value_get")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val _result = __self.value
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_get")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val _result = __self.variable
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__")
public fun namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT_WITH_PACKAGE
    val __newValue = newValue
    __self.variable = __newValue
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_foo")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_allocate")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS())
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_value_get")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_value_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS
    val _result = __self.my_value
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_get")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS
    val _result = __self.my_variable
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS.DEEPER_INSIDE_CLASS
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_foo")
public fun namespace_deeper_Foo_INSIDE_CLASS_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_init_allocate")
public fun namespace_deeper_Foo_INSIDE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Foo.INSIDE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_deeper_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Foo.INSIDE_CLASS())
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_my_value_get")
public fun namespace_deeper_Foo_INSIDE_CLASS_my_value_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS
    val _result = __self.my_value
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_my_variable_get")
public fun namespace_deeper_Foo_INSIDE_CLASS_my_variable_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS
    val _result = __self.my_variable
    return _result
}

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__")
public fun namespace_deeper_Foo_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo.INSIDE_CLASS
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_deeper_Foo_foo")
public fun namespace_deeper_Foo_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_deeper_Foo_init_allocate")
public fun namespace_deeper_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_deeper_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Foo())
}

@ExportedBridge("namespace_deeper_Foo_my_value_get")
public fun namespace_deeper_Foo_my_value_get(self: kotlin.native.internal.NativePtr): UInt {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo
    val _result = __self.my_value
    return _result
}

@ExportedBridge("namespace_deeper_Foo_my_variable_get")
public fun namespace_deeper_Foo_my_variable_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo
    val _result = __self.my_variable
    return _result
}

@ExportedBridge("namespace_deeper_Foo_my_variable_set__TypesOfArguments__Swift_Int64__")
public fun namespace_deeper_Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_deeper_NAMESPACED_CLASS_init_allocate")
public fun namespace_deeper_NAMESPACED_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.NAMESPACED_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_deeper_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.deeper.NAMESPACED_CLASS())
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_OBJECT_INSIDE_CLASS_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_OBJECT_INSIDE_CLASS_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_bar")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_bar(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE.Bar
    val _result = __self.bar()
    return _result
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_i_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_i_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE.Bar
    val _result = __self.i
    return _result
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_allocate")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.OBJECT_WITH_PACKAGE.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, i: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __i = i
    kotlin.native.internal.initInstance(____kt, namespace.deeper.OBJECT_WITH_PACKAGE.Bar(__i))
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.OBJECT_WITH_PACKAGE.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.deeper.OBJECT_WITH_PACKAGE.Foo())
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_OBJECT_INSIDE_OBJECT_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_OBJECT_INSIDE_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_foo")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE
    val _result = __self.foo()
    return _result
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.OBJECT_WITH_PACKAGE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_value_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE
    val _result = __self.value
    return _result
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_variable_get")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_variable_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE
    val _result = __self.variable
    return _result
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.OBJECT_WITH_PACKAGE
    val __newValue = newValue
    __self.variable = __newValue
}

@ExportedBridge("why_we_need_module_names_CLASS_WITH_SAME_NAME_foo")
public fun why_we_need_module_names_CLASS_WITH_SAME_NAME_foo(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as why_we_need_module_names.CLASS_WITH_SAME_NAME
    __self.foo()
}

@ExportedBridge("why_we_need_module_names_CLASS_WITH_SAME_NAME_init_allocate")
public fun why_we_need_module_names_CLASS_WITH_SAME_NAME_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<why_we_need_module_names.CLASS_WITH_SAME_NAME>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__")
public fun why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, why_we_need_module_names.CLASS_WITH_SAME_NAME())
}

@ExportedBridge("why_we_need_module_names_bar")
public fun why_we_need_module_names_bar(): Int {
    val _result = why_we_need_module_names.bar()
    return _result
}

@ExportedBridge("why_we_need_module_names_foo")
public fun why_we_need_module_names_foo(): kotlin.native.internal.NativePtr {
    val _result = why_we_need_module_names.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

