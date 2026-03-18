@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ignored.VALUE_CLASS::class, "22ExportedKotlinPackages7ignoredO4mainE11VALUE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Class_with_package::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE18Class_with_packageC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Class_with_package.INNER_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE18Class_with_packageC11INNER_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Class_with_package.INNER_OBJECT::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE18Class_with_packageC12INNER_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.DATA_OBJECT::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE11DATA_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Object_with_package::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19Object_with_packageC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Object_with_package.INNER_CLASS::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19Object_with_packageC11INNER_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.deeper.Object_with_package.INNER_OBJECT::class, "22ExportedKotlinPackages9namespaceO6deeperO4mainE19Object_with_packageC12INNER_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ABSTRACT_CLASS::class, "4main14ABSTRACT_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Class_without_package::class, "4main21Class_without_packageC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Class_without_package.INNER_CLASS::class, "4main21Class_without_packageC11INNER_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Class_without_package.INNER_OBJECT::class, "4main21Class_without_packageC12INNER_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DATA_CLASS::class, "4main10DATA_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Demo::class, "4main4DemoC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Demo.INNER_CLASS::class, "4main4DemoC11INNER_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Demo.INNER_OBJECT::class, "4main4DemoC12INNER_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OPEN_CLASS::class, "4main10OPEN_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Object_without_package::class, "4main22Object_without_packageC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Object_without_package.INNER_CLASS::class, "4main22Object_without_packageC11INNER_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Object_without_package.INNER_OBJECT::class, "4main22Object_without_packageC12INNER_OBJECTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(INTERFACE::class, "_INTERFACE")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Class_without_package_INNER_CLASS_init_allocate")
public fun Class_without_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Class_without_package.INNER_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Class_without_package.INNER_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("Class_without_package_INNER_OBJECT_get")
public fun Class_without_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { Class_without_package.INNER_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("Demo_INNER_CLASS_init_allocate")
public fun Demo_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Demo.INNER_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun Demo_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Demo.INNER_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("Demo_INNER_OBJECT_get")
public fun Demo_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { Demo.INNER_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg1_get")
public fun Demo_arg1_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.arg1 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg2_get")
public fun Demo_arg2_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.arg2 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg3_get")
public fun Demo_arg3_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.arg3 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg4_get")
public fun Demo_arg4_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.arg4 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun Demo_combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    val _result = run { __self.combine(__arg1, __arg2, __arg3, __arg4) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine_inner_classses__TypesOfArguments__main_Class_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_CLASS_main_Object_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_CLASS__")
public fun Demo_combine_inner_classses__TypesOfArguments__main_Class_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_CLASS_main_Object_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_CLASS__(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package.INNER_CLASS
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package.INNER_CLASS
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package.INNER_CLASS
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package.INNER_CLASS
    val _result = run { __self.combine_inner_classses(__arg1, __arg2, __arg3, __arg4) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine_inner_objects__TypesOfArguments__main_Class_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_OBJECT_main_Object_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_OBJECT__")
public fun Demo_combine_inner_objects__TypesOfArguments__main_Class_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_OBJECT_main_Object_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_OBJECT__(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package.INNER_OBJECT
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package.INNER_OBJECT
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package.INNER_OBJECT
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package.INNER_OBJECT
    val _result = run { __self.combine_inner_objects(__arg1, __arg2, __arg3, __arg4) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var1_get")
public fun Demo_var1_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.var1 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var1_set__TypesOfArguments__main_Class_without_package__")
public fun Demo_var1_set__TypesOfArguments__main_Class_without_package__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Class_without_package
    val _result = run { __self.var1 = __newValue }
    return run { _result; true }
}

@ExportedBridge("Demo_var2_get")
public fun Demo_var2_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.var2 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var2_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__")
public fun Demo_var2_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Class_with_package
    val _result = run { __self.var2 = __newValue }
    return run { _result; true }
}

@ExportedBridge("Demo_var3_get")
public fun Demo_var3_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.var3 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var3_set__TypesOfArguments__main_Object_without_package__")
public fun Demo_var3_set__TypesOfArguments__main_Object_without_package__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Object_without_package
    val _result = run { __self.var3 = __newValue }
    return run { _result; true }
}

@ExportedBridge("Demo_var4_get")
public fun Demo_var4_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = run { __self.var4 }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var4_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun Demo_var4_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Object_with_package
    val _result = run { __self.var4 = __newValue }
    return run { _result; true }
}

@ExportedBridge("Object_without_package_INNER_CLASS_init_allocate")
public fun Object_without_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Object_without_package.INNER_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Object_without_package.INNER_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("Object_without_package_INNER_OBJECT_get")
public fun Object_without_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { Object_without_package.INNER_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Class_without_package_init_allocate")
public fun __root___Class_without_package_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Class_without_package>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Class_without_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Class_without_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Class_without_package()) }
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

@ExportedBridge("__root___Demo_init_allocate")
public fun __root___Demo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Demo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(__kt: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    val _result = run { kotlin.native.internal.initInstance(____kt, Demo(__arg1, __arg2, __arg3, __arg4)) }
    return run { _result; true }
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

@ExportedBridge("__root___Object_without_package_get")
public fun __root___Object_without_package_get(): kotlin.native.internal.NativePtr {
    val _result = run { Object_without_package }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun __root___combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): Boolean {
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    val _result = run { combine(__arg1, __arg2, __arg3, __arg4) }
    return run { _result; true }
}

@ExportedBridge("__root___extensionOnNullabeRef__TypesOfArguments__Swift_Optional_main_Class_without_package___")
public fun __root___extensionOnNullabeRef__TypesOfArguments__Swift_Optional_main_Class_without_package___(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Class_without_package
    val _result = run { __receiver.extensionOnNullabeRef() }
    return run { _result; true }
}

@ExportedBridge("__root___extensionOnNullablePrimitive__TypesOfArguments__Swift_Optional_Swift_Int32___")
public fun __root___extensionOnNullablePrimitive__TypesOfArguments__Swift_Optional_Swift_Int32___(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(`receiver`)
    val _result = run { __receiver.extensionOnNullablePrimitive() }
    return run { _result; true }
}

@ExportedBridge("__root___extensionVarOnNullablePrimitive_get__TypesOfArguments__Swift_Optional_Swift_Int32___")
public fun __root___extensionVarOnNullablePrimitive_get__TypesOfArguments__Swift_Optional_Swift_Int32___(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(`receiver`)
    val _result = run { __receiver.extensionVarOnNullablePrimitive }
    return _result.objcPtr()
}

@ExportedBridge("__root___extensionVarOnNullablePrimitive_set__TypesOfArguments__Swift_Optional_Swift_Int32__Swift_String__")
public fun __root___extensionVarOnNullablePrimitive_set__TypesOfArguments__Swift_Optional_Swift_Int32__Swift_String__(`receiver`: kotlin.native.internal.NativePtr, v: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(`receiver`)
    val __v = interpretObjCPointer<kotlin.String>(v)
    val _result = run { __receiver.extensionVarOnNullablePrimitive = __v }
    return run { _result; true }
}

@ExportedBridge("__root___extensionVarOnNullableRef_get__TypesOfArguments__Swift_Optional_main_Class_without_package___")
public fun __root___extensionVarOnNullableRef_get__TypesOfArguments__Swift_Optional_main_Class_without_package___(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Class_without_package
    val _result = run { __receiver.extensionVarOnNullableRef }
    return _result.objcPtr()
}

@ExportedBridge("__root___extensionVarOnNullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package__Swift_String__")
public fun __root___extensionVarOnNullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package__Swift_String__(`receiver`: kotlin.native.internal.NativePtr, v: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Class_without_package
    val __v = interpretObjCPointer<kotlin.String>(v)
    val _result = run { __receiver.extensionVarOnNullableRef = __v }
    return run { _result; true }
}

@ExportedBridge("__root___nullablePrim_get")
public fun __root___nullablePrim_get(): kotlin.native.internal.NativePtr {
    val _result = run { nullablePrim }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("__root___nullablePrim_set__TypesOfArguments__Swift_Optional_Swift_Int32___")
public fun __root___nullablePrim_set__TypesOfArguments__Swift_Optional_Swift_Int32___(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(newValue)
    val _result = run { nullablePrim = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___nullableRef_get")
public fun __root___nullableRef_get(): kotlin.native.internal.NativePtr {
    val _result = run { nullableRef }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package___")
public fun __root___nullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package___(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Class_without_package
    val _result = run { nullableRef = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___nullable_input_prim__TypesOfArguments__Swift_Optional_Swift_Int32___")
public fun __root___nullable_input_prim__TypesOfArguments__Swift_Optional_Swift_Int32___(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = if (i == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(i)
    val _result = run { nullable_input_prim(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___nullable_input_ref__TypesOfArguments__Swift_Optional_main_Class_without_package___")
public fun __root___nullable_input_ref__TypesOfArguments__Swift_Optional_main_Class_without_package___(i: kotlin.native.internal.NativePtr): Boolean {
    val __i = if (i == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(i) as Class_without_package
    val _result = run { nullable_input_ref(__i) }
    return run { _result; true }
}

@ExportedBridge("__root___nullable_output_prim")
public fun __root___nullable_output_prim(): kotlin.native.internal.NativePtr {
    val _result = run { nullable_output_prim() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("__root___nullable_output_ref")
public fun __root___nullable_output_ref(): kotlin.native.internal.NativePtr {
    val _result = run { nullable_output_ref() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_ABSTRACT_CLASS")
public fun __root___produce_ABSTRACT_CLASS(): kotlin.native.internal.NativePtr {
    val _result = run { produce_ABSTRACT_CLASS() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_DATA_CLASS")
public fun __root___produce_DATA_CLASS(): kotlin.native.internal.NativePtr {
    val _result = run { produce_DATA_CLASS() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_DATA_OBJECT")
public fun __root___produce_DATA_OBJECT(): kotlin.native.internal.NativePtr {
    val _result = run { produce_DATA_OBJECT() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_INTERFACE")
public fun __root___produce_INTERFACE(): kotlin.native.internal.NativePtr {
    val _result = run { produce_INTERFACE() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_OPEN_CLASS")
public fun __root___produce_OPEN_CLASS(): kotlin.native.internal.NativePtr {
    val _result = run { produce_OPEN_CLASS() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_class")
public fun __root___produce_class(): kotlin.native.internal.NativePtr {
    val _result = run { produce_class() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_class_wp")
public fun __root___produce_class_wp(): kotlin.native.internal.NativePtr {
    val _result = run { produce_class_wp() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_object")
public fun __root___produce_object(): kotlin.native.internal.NativePtr {
    val _result = run { produce_object() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_object_wp")
public fun __root___produce_object_wp(): kotlin.native.internal.NativePtr {
    val _result = run { produce_object_wp() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___receive_ABSTRACT_CLASS__TypesOfArguments__main_ABSTRACT_CLASS__")
public fun __root___receive_ABSTRACT_CLASS__TypesOfArguments__main_ABSTRACT_CLASS__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as ABSTRACT_CLASS
    val _result = run { receive_ABSTRACT_CLASS(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___receive_DATA_CLASS__TypesOfArguments__main_DATA_CLASS__")
public fun __root___receive_DATA_CLASS__TypesOfArguments__main_DATA_CLASS__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as DATA_CLASS
    val _result = run { receive_DATA_CLASS(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___receive_INTERFACE__TypesOfArguments__anyU20main_INTERFACE__")
public fun __root___receive_INTERFACE__TypesOfArguments__anyU20main_INTERFACE__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as INTERFACE
    val _result = run { receive_INTERFACE(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_DATA_OBJECT__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_DATA_OBJECT__")
public fun __root___recieve_DATA_OBJECT__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_DATA_OBJECT__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as namespace.deeper.DATA_OBJECT
    val _result = run { recieve_DATA_OBJECT(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_OPEN_CLASS__TypesOfArguments__main_OPEN_CLASS__")
public fun __root___recieve_OPEN_CLASS__TypesOfArguments__main_OPEN_CLASS__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as OPEN_CLASS
    val _result = run { recieve_OPEN_CLASS(__x) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_class__TypesOfArguments__main_Class_without_package__")
public fun __root___recieve_class__TypesOfArguments__main_Class_without_package__(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Class_without_package
    val _result = run { recieve_class(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_class_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__")
public fun __root___recieve_class_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as namespace.deeper.Class_with_package
    val _result = run { recieve_class_wp(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_object__TypesOfArguments__main_Object_without_package__")
public fun __root___recieve_object__TypesOfArguments__main_Object_without_package__(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Object_without_package
    val _result = run { recieve_object(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___recieve_object_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun __root___recieve_object_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg: kotlin.native.internal.NativePtr): Boolean {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as namespace.deeper.Object_with_package
    val _result = run { recieve_object_wp(__arg) }
    return run { _result; true }
}

@ExportedBridge("__root___val_class_get")
public fun __root___val_class_get(): kotlin.native.internal.NativePtr {
    val _result = run { val_class }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_class_wp_get")
public fun __root___val_class_wp_get(): kotlin.native.internal.NativePtr {
    val _result = run { val_class_wp }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_object_get")
public fun __root___val_object_get(): kotlin.native.internal.NativePtr {
    val _result = run { val_object }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_object_wp_get")
public fun __root___val_object_wp_get(): kotlin.native.internal.NativePtr {
    val _result = run { val_object_wp }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_get")
public fun __root___var_class_get(): kotlin.native.internal.NativePtr {
    val _result = run { var_class }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_set__TypesOfArguments__main_Class_without_package__")
public fun __root___var_class_set__TypesOfArguments__main_Class_without_package__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Class_without_package
    val _result = run { var_class = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___var_class_wp_get")
public fun __root___var_class_wp_get(): kotlin.native.internal.NativePtr {
    val _result = run { var_class_wp }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__")
public fun __root___var_class_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Class_with_package
    val _result = run { var_class_wp = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___var_object_get")
public fun __root___var_object_get(): kotlin.native.internal.NativePtr {
    val _result = run { var_object }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_object_set__TypesOfArguments__main_Object_without_package__")
public fun __root___var_object_set__TypesOfArguments__main_Object_without_package__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Object_without_package
    val _result = run { var_object = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___var_object_wp_get")
public fun __root___var_object_wp_get(): kotlin.native.internal.NativePtr {
    val _result = run { var_object_wp }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_object_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__")
public fun __root___var_object_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Object_with_package
    val _result = run { var_object_wp = __newValue }
    return run { _result; true }
}

@ExportedBridge("ignored_ENUM_A")
public fun ignored_ENUM_A(): kotlin.native.internal.NativePtr {
    val _result = run { ignored.ENUM.A }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ignored_VALUE_CLASS_a_get")
public fun ignored_VALUE_CLASS_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ignored.VALUE_CLASS
    val _result = run { __self.a }
    return _result
}

@ExportedBridge("ignored_VALUE_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun ignored_VALUE_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ignored.VALUE_CLASS
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("ignored_VALUE_CLASS_hashCode")
public fun ignored_VALUE_CLASS_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ignored.VALUE_CLASS
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("ignored_VALUE_CLASS_init_allocate")
public fun ignored_VALUE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ignored.VALUE_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ignored_VALUE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun ignored_VALUE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, a: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = a
    val _result = run { kotlin.native.internal.initInstance(____kt, ignored.VALUE_CLASS(__a)) }
    return run { _result; true }
}

@ExportedBridge("ignored_VALUE_CLASS_toString")
public fun ignored_VALUE_CLASS_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ignored.VALUE_CLASS
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("ignored_produce_ENUM")
public fun ignored_produce_ENUM(): kotlin.native.internal.NativePtr {
    val _result = run { ignored.produce_ENUM() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ignored_produce_VALUE_CLASS")
public fun ignored_produce_VALUE_CLASS(): kotlin.native.internal.NativePtr {
    val _result = run { ignored.produce_VALUE_CLASS() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("ignored_receive_ENUM__TypesOfArguments__ExportedKotlinPackages_ignored_ENUM__")
public fun ignored_receive_ENUM__TypesOfArguments__ExportedKotlinPackages_ignored_ENUM__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as ignored.ENUM
    val _result = run { ignored.receive_ENUM(__x) }
    return run { _result; true }
}

@ExportedBridge("ignored_receive_VALUE_CLASS__TypesOfArguments__ExportedKotlinPackages_ignored_VALUE_CLASS__")
public fun ignored_receive_VALUE_CLASS__TypesOfArguments__ExportedKotlinPackages_ignored_VALUE_CLASS__(x: kotlin.native.internal.NativePtr): Boolean {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as ignored.VALUE_CLASS
    val _result = run { ignored.receive_VALUE_CLASS(__x) }
    return run { _result; true }
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_CLASS_init_allocate")
public fun namespace_deeper_Class_with_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.deeper.Class_with_package.INNER_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.deeper.Class_with_package.INNER_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_OBJECT_get")
public fun namespace_deeper_Class_with_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { namespace.deeper.Class_with_package.INNER_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_init_allocate")
public fun namespace_deeper_Class_with_package_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.deeper.Class_with_package>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.deeper.Class_with_package()) }
    return run { _result; true }
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_a_get")
public fun namespace_deeper_DATA_OBJECT_a_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT
    val _result = run { __self.a }
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun namespace_deeper_DATA_OBJECT_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_get")
public fun namespace_deeper_DATA_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { namespace.deeper.DATA_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_hashCode")
public fun namespace_deeper_DATA_OBJECT_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("namespace_deeper_DATA_OBJECT_toString")
public fun namespace_deeper_DATA_OBJECT_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.DATA_OBJECT
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_CLASS_init_allocate")
public fun namespace_deeper_Object_with_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.deeper.Object_with_package.INNER_CLASS>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.deeper.Object_with_package.INNER_CLASS()) }
    return run { _result; true }
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_OBJECT_get")
public fun namespace_deeper_Object_with_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = run { namespace.deeper.Object_with_package.INNER_OBJECT }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Object_with_package_get")
public fun namespace_deeper_Object_with_package_get(): kotlin.native.internal.NativePtr {
    val _result = run { namespace.deeper.Object_with_package }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
