@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.Foo::class, "22ExportedKotlinPackages11typealiasesO4mainE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.inner.Bar::class, "22ExportedKotlinPackages11typealiasesO5innerO4mainE3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ABSTRACT_CLASS::class, "4main14ABSTRACT_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS::class, "4main10DATA_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS_WITH_REF::class, "4main19DATA_CLASS_WITH_REFC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_OBJECT_WITH_PACKAGE::class, "4main24DATA_OBJECT_WITH_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM::class, "4main4ENUMC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM.INSIDE_ENUM::class, "4main4ENUMC11INSIDE_ENUMC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INHERITANCE_SINGLE_CLASS::class, "4main24INHERITANCE_SINGLE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_CLASS_INHERITANCE::class, "4main29OBJECT_WITH_CLASS_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_INTERFACE_INHERITANCE::class, "4main33OBJECT_WITH_INTERFACE_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OPEN_CLASS::class, "4main10OPEN_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED::class, "4main6SEALEDC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED.O::class, "4main6SEALEDC1OC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

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

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_foo")
public fun DATA_OBJECT_WITH_PACKAGE_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = __self.foo()
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_hashCode")
public fun DATA_OBJECT_WITH_PACKAGE_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_toString")
public fun DATA_OBJECT_WITH_PACKAGE_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_value_get")
public fun DATA_OBJECT_WITH_PACKAGE_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = __self.value
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_variable_get")
public fun DATA_OBJECT_WITH_PACKAGE_variable_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = __self.variable
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__")
public fun DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val __newValue = newValue
    __self.variable = __newValue
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

@ExportedBridge("SEALED_O_get")
public fun SEALED_O_get(): kotlin.native.internal.NativePtr {
    val _result = SEALED.O
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("__root___DATA_OBJECT_WITH_PACKAGE_get")
public fun __root___DATA_OBJECT_WITH_PACKAGE_get(): kotlin.native.internal.NativePtr {
    val _result = DATA_OBJECT_WITH_PACKAGE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_allocate")
public fun __root___INHERITANCE_SINGLE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<INHERITANCE_SINGLE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, INHERITANCE_SINGLE_CLASS())
}

@ExportedBridge("__root___OBJECT_WITH_CLASS_INHERITANCE_get")
public fun __root___OBJECT_WITH_CLASS_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_WITH_CLASS_INHERITANCE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OBJECT_WITH_INTERFACE_INHERITANCE_get")
public fun __root___OBJECT_WITH_INTERFACE_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_WITH_INTERFACE_INHERITANCE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_allocate")
public fun __root___OPEN_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OPEN_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OPEN_CLASS())
}

@ExportedBridge("__root___increment__TypesOfArguments__Swift_Int32__")
public fun __root___increment__TypesOfArguments__Swift_Int32__(integer: Int): Int {
    val __integer = integer
    val _result = increment(__integer)
    return _result
}

@ExportedBridge("typealiases_Foo_init_allocate")
public fun typealiases_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<typealiases.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun typealiases_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, typealiases.Foo())
}

@ExportedBridge("typealiases_inner_Bar_init_allocate")
public fun typealiases_inner_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<typealiases.`inner`.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UInt__")
public fun typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, typealiases.`inner`.Bar())
}

