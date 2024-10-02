@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.Foo::class, "22ExportedKotlinPackages11typealiasesO4mainE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.inner.Bar::class, "22ExportedKotlinPackages11typealiasesO5innerO4mainE3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ABSTRACT_CLASS::class, "4main14ABSTRACT_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_OBJECT_WITH_PACKAGE::class, "4main24DATA_OBJECT_WITH_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INHERITANCE_SINGLE_CLASS::class, "4main24INHERITANCE_SINGLE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_CLASS_INHERITANCE::class, "4main29OBJECT_WITH_CLASS_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OPEN_CLASS::class, "4main10OPEN_CLASSC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

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
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, INHERITANCE_SINGLE_CLASS())
}

@ExportedBridge("__root___OBJECT_WITH_CLASS_INHERITANCE_get")
public fun __root___OBJECT_WITH_CLASS_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_WITH_CLASS_INHERITANCE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_allocate")
public fun __root___OPEN_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OPEN_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, typealiases.Foo())
}

@ExportedBridge("typealiases_inner_Bar_init_allocate")
public fun typealiases_inner_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<typealiases.inner.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UInt__")
public fun typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, typealiases.inner.Bar())
}

