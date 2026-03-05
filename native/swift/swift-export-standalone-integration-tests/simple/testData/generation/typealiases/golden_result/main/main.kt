@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.Foo::class, "22ExportedKotlinPackages11typealiasesO4mainE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(typealiases.inner.Bar::class, "22ExportedKotlinPackages11typealiasesO5innerO4mainE3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ABSTRACT_CLASS::class, "4main14ABSTRACT_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS::class, "4main10DATA_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS_WITH_REF::class, "4main19DATA_CLASS_WITH_REFC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_OBJECT_WITH_PACKAGE::class, "4main24DATA_OBJECT_WITH_PACKAGEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ENUM.INSIDE_ENUM::class, "4main4ENUMO11INSIDE_ENUMC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GENERIC_CLASS::class, "4main13GENERIC_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INHERITANCE_SINGLE_CLASS::class, "4main24INHERITANCE_SINGLE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INLINE_CLASS::class, "4main12INLINE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INLINE_CLASS_WITH_REF::class, "4main21INLINE_CLASS_WITH_REFC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_CLASS_INHERITANCE::class, "4main29OBJECT_WITH_CLASS_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_GENERIC_INHERITANCE::class, "4main31OBJECT_WITH_GENERIC_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_INTERFACE_INHERITANCE::class, "4main33OBJECT_WITH_INTERFACE_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OPEN_CLASS::class, "4main10OPEN_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED::class, "4main6SEALEDC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SEALED.O::class, "4main6SEALEDC1OC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OUTSIDE_PROTO::class, "_OUTSIDE_PROTO")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("DATA_CLASS_WITH_REF_copy__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun DATA_CLASS_WITH_REF_copy__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, o: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val __o = kotlin.native.internal.ref.dereferenceExternalRCRef(o) as kotlin.Any
    val _result = run { __self.copy(__o) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_REF_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun DATA_CLASS_WITH_REF_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_REF_hashCode")
public fun DATA_CLASS_WITH_REF_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("DATA_CLASS_WITH_REF_o_get")
public fun DATA_CLASS_WITH_REF_o_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = run { __self.o }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_WITH_REF_toString")
public fun DATA_CLASS_WITH_REF_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS_WITH_REF
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("DATA_CLASS_a_get")
public fun DATA_CLASS_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = run { __self.a }
    return _result
}

@ExportedBridge("DATA_CLASS_copy__TypesOfArguments__Swift_Int32__")
public fun DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, a: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val __a = a
    val _result = run { __self.copy(__a) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("DATA_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun DATA_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("DATA_CLASS_hashCode")
public fun DATA_CLASS_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("DATA_CLASS_toString")
public fun DATA_CLASS_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_CLASS
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun DATA_OBJECT_WITH_PACKAGE_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_foo")
public fun DATA_OBJECT_WITH_PACKAGE_foo(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = run { __self.foo() }
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_hashCode")
public fun DATA_OBJECT_WITH_PACKAGE_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_toString")
public fun DATA_OBJECT_WITH_PACKAGE_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_value_get")
public fun DATA_OBJECT_WITH_PACKAGE_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = run { __self.value }
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_variable_get")
public fun DATA_OBJECT_WITH_PACKAGE_variable_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val _result = run { __self.variable }
    return _result
}

@ExportedBridge("DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__")
public fun DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as DATA_OBJECT_WITH_PACKAGE
    val __newValue = newValue
    val _result = run { __self.variable = __newValue }
    return run { _result; true }
}

@ExportedBridge("ENUM_A")
public fun ENUM_A(): kotlin.native.internal.NativePtr {
    val _result = run { ENUM.A }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_B")
public fun ENUM_B(): kotlin.native.internal.NativePtr {
    val _result = run { ENUM.B }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_C")
public fun ENUM_C(): kotlin.native.internal.NativePtr {
    val _result = run { ENUM.C }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_INSIDE_ENUM_init_allocate")
public fun ENUM_INSIDE_ENUM_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ENUM.INSIDE_ENUM>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ENUM.INSIDE_ENUM()) }
    return run { _result; true }
}

@ExportedBridge("INLINE_CLASS_WITH_REF_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun INLINE_CLASS_WITH_REF_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS_WITH_REF
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("INLINE_CLASS_WITH_REF_hashCode")
public fun INLINE_CLASS_WITH_REF_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS_WITH_REF
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("INLINE_CLASS_WITH_REF_i_get")
public fun INLINE_CLASS_WITH_REF_i_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS_WITH_REF
    val _result = run { __self.i }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("INLINE_CLASS_WITH_REF_toString")
public fun INLINE_CLASS_WITH_REF_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS_WITH_REF
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("INLINE_CLASS_a_get")
public fun INLINE_CLASS_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS
    val _result = run { __self.a }
    return _result
}

@ExportedBridge("INLINE_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun INLINE_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("INLINE_CLASS_hashCode")
public fun INLINE_CLASS_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("INLINE_CLASS_toString")
public fun INLINE_CLASS_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INLINE_CLASS
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_hasNext")
public fun OBJECT_WITH_GENERIC_INHERITANCE_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.hasNext() }
    return _result
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_hasPrevious")
public fun OBJECT_WITH_GENERIC_INHERITANCE_hasPrevious(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.hasPrevious() }
    return _result
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_next")
public fun OBJECT_WITH_GENERIC_INHERITANCE_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.next() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_nextIndex")
public fun OBJECT_WITH_GENERIC_INHERITANCE_nextIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.nextIndex() }
    return _result
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_previous")
public fun OBJECT_WITH_GENERIC_INHERITANCE_previous(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.previous() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_WITH_GENERIC_INHERITANCE_previousIndex")
public fun OBJECT_WITH_GENERIC_INHERITANCE_previousIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_WITH_GENERIC_INHERITANCE
    val _result = run { __self.previousIndex() }
    return _result
}

@ExportedBridge("SEALED_O_get")
public fun SEALED_O_get(): kotlin.native.internal.NativePtr {
    val _result = run { SEALED.O }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_WITH_REF_init_allocate")
public fun __root___DATA_CLASS_WITH_REF_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<DATA_CLASS_WITH_REF>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(__kt: kotlin.native.internal.NativePtr, o: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __o = kotlin.native.internal.ref.dereferenceExternalRCRef(o) as kotlin.Any
    val _result = run { kotlin.native.internal.initInstance(____kt, DATA_CLASS_WITH_REF(__o)) }
    return run { _result; true }
}

@ExportedBridge("__root___DATA_CLASS_init_allocate")
public fun __root___DATA_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<DATA_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, a: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    val _result = run { kotlin.native.internal.initInstance(____kt, DATA_CLASS(__a)) }
    return run { _result; true }
}

@ExportedBridge("__root___DATA_OBJECT_WITH_PACKAGE_get")
public fun __root___DATA_OBJECT_WITH_PACKAGE_get(): kotlin.native.internal.NativePtr {
    val _result = run { DATA_OBJECT_WITH_PACKAGE }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GENERIC_CLASS_init_allocate")
public fun __root___GENERIC_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<GENERIC_CLASS<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GENERIC_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___GENERIC_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, GENERIC_CLASS<kotlin.Any?>()) }
    return run { _result; true }
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_allocate")
public fun __root___INHERITANCE_SINGLE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<INHERITANCE_SINGLE_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, INHERITANCE_SINGLE_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("__root___INLINE_CLASS_WITH_REF_init_allocate")
public fun __root___INLINE_CLASS_WITH_REF_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<INLINE_CLASS_WITH_REF>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INLINE_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_DATA_CLASS_WITH_REF__")
public fun __root___INLINE_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_DATA_CLASS_WITH_REF__(__kt: kotlin.native.internal.NativePtr, i: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __i = kotlin.native.internal.ref.dereferenceExternalRCRef(i) as DATA_CLASS_WITH_REF
    val _result = run { kotlin.native.internal.initInstance(____kt, INLINE_CLASS_WITH_REF(__i)) }
    return run { _result; true }
}

@ExportedBridge("__root___INLINE_CLASS_init_allocate")
public fun __root___INLINE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<INLINE_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INLINE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___INLINE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, a: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    val _result = run { kotlin.native.internal.initInstance(____kt, INLINE_CLASS(__a)) }
    return run { _result; true }
}

@ExportedBridge("__root___OBJECT_WITH_CLASS_INHERITANCE_get")
public fun __root___OBJECT_WITH_CLASS_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = run { OBJECT_WITH_CLASS_INHERITANCE }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OBJECT_WITH_GENERIC_INHERITANCE_get")
public fun __root___OBJECT_WITH_GENERIC_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = run { OBJECT_WITH_GENERIC_INHERITANCE }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OBJECT_WITH_INTERFACE_INHERITANCE_get")
public fun __root___OBJECT_WITH_INTERFACE_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = run { OBJECT_WITH_INTERFACE_INHERITANCE }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_allocate")
public fun __root___OPEN_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<OPEN_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, OPEN_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("__root___block_get")
public fun __root___block_get(): kotlin.native.internal.NativePtr {
    val _result = run { block }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___block_set__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___block_set__TypesOfArguments__U2829202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(newValue);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { block = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___consume_closure__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___consume_closure__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(block);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { consume_closure(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___deeper_closure_typealiase__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___deeper_closure_typealiase__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(block);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { deeper_closure_typealiase(__block) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___increment__TypesOfArguments__Swift_Int32__")
public fun __root___increment__TypesOfArguments__Swift_Int32__(integer: Int): Int {
    val __integer = integer
    val _result = run { increment(__integer) }
    return _result
}

@ExportedBridge("__root___produce_closure")
public fun __root___produce_closure(): kotlin.native.internal.NativePtr {
    val _result = run { produce_closure() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Unit>).invoke() }
    return run { _result; true }
}

@ExportedBridge("typealiases_Foo_init_allocate")
public fun typealiases_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<typealiases.Foo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun typealiases_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, typealiases.Foo()) }
    return run { _result; true }
}

@ExportedBridge("typealiases_inner_Bar_init_allocate")
public fun typealiases_inner_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<typealiases.inner.Bar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, typealiases.inner.Bar()) }
    return run { _result; true }
}
