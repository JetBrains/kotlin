import kotlin.native.internal.ExportedBridge

@ExportedBridge("Class_without_package_INNER_CLASS_init_allocate")
public fun Class_without_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Class_without_package.INNER_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun Class_without_package_INNER_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Class_without_package.INNER_CLASS())
}

@ExportedBridge("Class_without_package_INNER_OBJECT_get")
public fun Class_without_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = Class_without_package.INNER_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_INNER_CLASS_init_allocate")
public fun Demo_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Demo.INNER_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun Demo_INNER_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Demo.INNER_CLASS())
}

@ExportedBridge("Demo_INNER_OBJECT_get")
public fun Demo_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = Demo.INNER_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg1_get")
public fun Demo_arg1_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.arg1
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg2_get")
public fun Demo_arg2_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.arg2
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg3_get")
public fun Demo_arg3_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.arg3
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_arg4_get")
public fun Demo_arg4_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.arg4
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__")
public fun Demo_combine(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    val _result = __self.combine(__arg1, __arg2, __arg3, __arg4)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine_inner_classses__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__")
public fun Demo_combine_inner_classses(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package.INNER_CLASS
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package.INNER_CLASS
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package.INNER_CLASS
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package.INNER_CLASS
    val _result = __self.combine_inner_classses(__arg1, __arg2, __arg3, __arg4)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_combine_inner_objects__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__")
public fun Demo_combine_inner_objects(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package.INNER_OBJECT
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package.INNER_OBJECT
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package.INNER_OBJECT
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package.INNER_OBJECT
    val _result = __self.combine_inner_objects(__arg1, __arg2, __arg3, __arg4)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var1_get")
public fun Demo_var1_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.var1
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var1_set__TypesOfArguments__uintptr_t__")
public fun Demo_var1_set(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Class_without_package
    __self.var1 = __newValue
}

@ExportedBridge("Demo_var2_get")
public fun Demo_var2_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.var2
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var2_set__TypesOfArguments__uintptr_t__")
public fun Demo_var2_set(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Class_with_package
    __self.var2 = __newValue
}

@ExportedBridge("Demo_var3_get")
public fun Demo_var3_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.var3
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var3_set__TypesOfArguments__uintptr_t__")
public fun Demo_var3_set(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Object_without_package
    __self.var3 = __newValue
}

@ExportedBridge("Demo_var4_get")
public fun Demo_var4_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.var4
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Demo_var4_set__TypesOfArguments__uintptr_t__")
public fun Demo_var4_set(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Object_with_package
    __self.var4 = __newValue
}

@ExportedBridge("Object_without_package_INNER_CLASS_init_allocate")
public fun Object_without_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Object_without_package.INNER_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun Object_without_package_INNER_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Object_without_package.INNER_CLASS())
}

@ExportedBridge("Object_without_package_INNER_OBJECT_get")
public fun Object_without_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = Object_without_package.INNER_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Class_without_package_init_allocate")
public fun __root___Class_without_package_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Class_without_package>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Class_without_package_init_initialize__TypesOfArguments__uintptr_t__")
public fun __root___Class_without_package_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Class_without_package())
}

@ExportedBridge("__root___Demo_init_allocate")
public fun __root___Demo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Demo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Demo_init_initialize__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t_uintptr_t__")
public fun __root___Demo_init_initialize(__kt: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    kotlin.native.internal.initInstance(____kt, Demo(__arg1, __arg2, __arg3, __arg4))
}

@ExportedBridge("__root___Object_without_package_get")
public fun __root___Object_without_package_get(): kotlin.native.internal.NativePtr {
    val _result = Object_without_package
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__")
public fun __root___combine(arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr): Unit {
    val __arg1 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg1) as Class_without_package
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as namespace.deeper.Class_with_package
    val __arg3 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg3) as Object_without_package
    val __arg4 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg4) as namespace.deeper.Object_with_package
    combine(__arg1, __arg2, __arg3, __arg4)
}

@ExportedBridge("__root___produce_class")
public fun __root___produce_class(): kotlin.native.internal.NativePtr {
    val _result = produce_class()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_class_wp")
public fun __root___produce_class_wp(): kotlin.native.internal.NativePtr {
    val _result = produce_class_wp()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_object")
public fun __root___produce_object(): kotlin.native.internal.NativePtr {
    val _result = produce_object()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_object_wp")
public fun __root___produce_object_wp(): kotlin.native.internal.NativePtr {
    val _result = produce_object_wp()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___recieve_class__TypesOfArguments__uintptr_t__")
public fun __root___recieve_class(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Class_without_package
    recieve_class(__arg)
}

@ExportedBridge("__root___recieve_class_wp__TypesOfArguments__uintptr_t__")
public fun __root___recieve_class_wp(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as namespace.deeper.Class_with_package
    recieve_class_wp(__arg)
}

@ExportedBridge("__root___recieve_object__TypesOfArguments__uintptr_t__")
public fun __root___recieve_object(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Object_without_package
    recieve_object(__arg)
}

@ExportedBridge("__root___recieve_object_wp__TypesOfArguments__uintptr_t__")
public fun __root___recieve_object_wp(arg: kotlin.native.internal.NativePtr): Unit {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as namespace.deeper.Object_with_package
    recieve_object_wp(__arg)
}

@ExportedBridge("__root___val_class_get")
public fun __root___val_class_get(): kotlin.native.internal.NativePtr {
    val _result = val_class
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_class_wp_get")
public fun __root___val_class_wp_get(): kotlin.native.internal.NativePtr {
    val _result = val_class_wp
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_object_get")
public fun __root___val_object_get(): kotlin.native.internal.NativePtr {
    val _result = val_object
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___val_object_wp_get")
public fun __root___val_object_wp_get(): kotlin.native.internal.NativePtr {
    val _result = val_object_wp
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_get")
public fun __root___var_class_get(): kotlin.native.internal.NativePtr {
    val _result = var_class
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_set__TypesOfArguments__uintptr_t__")
public fun __root___var_class_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Class_without_package
    var_class = __newValue
}

@ExportedBridge("__root___var_class_wp_get")
public fun __root___var_class_wp_get(): kotlin.native.internal.NativePtr {
    val _result = var_class_wp
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_class_wp_set__TypesOfArguments__uintptr_t__")
public fun __root___var_class_wp_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Class_with_package
    var_class_wp = __newValue
}

@ExportedBridge("__root___var_object_get")
public fun __root___var_object_get(): kotlin.native.internal.NativePtr {
    val _result = var_object
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_object_set__TypesOfArguments__uintptr_t__")
public fun __root___var_object_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Object_without_package
    var_object = __newValue
}

@ExportedBridge("__root___var_object_wp_get")
public fun __root___var_object_wp_get(): kotlin.native.internal.NativePtr {
    val _result = var_object_wp
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___var_object_wp_set__TypesOfArguments__uintptr_t__")
public fun __root___var_object_wp_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as namespace.deeper.Object_with_package
    var_object_wp = __newValue
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_CLASS_init_allocate")
public fun namespace_deeper_Class_with_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Class_with_package.INNER_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Class_with_package_INNER_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Class_with_package.INNER_CLASS())
}

@ExportedBridge("namespace_deeper_Class_with_package_INNER_OBJECT_get")
public fun namespace_deeper_Class_with_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.Class_with_package.INNER_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_init_allocate")
public fun namespace_deeper_Class_with_package_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Class_with_package>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Class_with_package_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Class_with_package())
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_CLASS_init_allocate")
public fun namespace_deeper_Object_with_package_INNER_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.Object_with_package.INNER_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Object_with_package_INNER_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, namespace.deeper.Object_with_package.INNER_CLASS())
}

@ExportedBridge("namespace_deeper_Object_with_package_INNER_OBJECT_get")
public fun namespace_deeper_Object_with_package_INNER_OBJECT_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.Object_with_package.INNER_OBJECT
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_Object_with_package_get")
public fun namespace_deeper_Object_with_package_get(): kotlin.native.internal.NativePtr {
    val _result = namespace.deeper.Object_with_package
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

