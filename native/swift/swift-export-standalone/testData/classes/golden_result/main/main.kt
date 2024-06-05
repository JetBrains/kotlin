import kotlin.native.internal.ExportedBridge

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

@ExportedBridge("Foo_INSIDE_CLASS_init_allocate")
public fun Foo_INSIDE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo.INSIDE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun Foo_INSIDE_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("Foo_INSIDE_CLASS_my_variable_inner_set__TypesOfArguments__int64_t__")
public fun Foo_INSIDE_CLASS_my_variable_inner_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
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

@ExportedBridge("Foo_my_variable_set__TypesOfArguments__int64_t__")
public fun Foo_my_variable_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate")
public fun OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OBJECT_NO_PACKAGE.Bar.CLASS_INSIDE_CLASS_INSIDE_OBJECT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize__TypesOfArguments__uintptr_t__")
public fun OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, OBJECT_NO_PACKAGE.Bar.CLASS_INSIDE_CLASS_INSIDE_OBJECT())
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

@ExportedBridge("OBJECT_NO_PACKAGE_Bar_init_initialize__TypesOfArguments__uintptr_t_int32_t__")
public fun OBJECT_NO_PACKAGE_Bar_init_initialize(__kt: kotlin.native.internal.NativePtr, i: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    val __i = i
    kotlin.native.internal.initInstance(____kt, OBJECT_NO_PACKAGE.Bar(__i))
}

@ExportedBridge("OBJECT_NO_PACKAGE_Foo_init_allocate")
public fun OBJECT_NO_PACKAGE_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OBJECT_NO_PACKAGE.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("OBJECT_NO_PACKAGE_Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun OBJECT_NO_PACKAGE_Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("OBJECT_NO_PACKAGE_variable_set__TypesOfArguments__int32_t__")
public fun OBJECT_NO_PACKAGE_variable_set(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OBJECT_NO_PACKAGE
    val __newValue = newValue
    __self.variable = __newValue
}

@ExportedBridge("__root___CLASS_WITH_SAME_NAME_init_allocate")
public fun __root___CLASS_WITH_SAME_NAME_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<CLASS_WITH_SAME_NAME>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__uintptr_t__")
public fun __root___CLASS_WITH_SAME_NAME_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, CLASS_WITH_SAME_NAME())
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__uintptr_t_int32_t__")
public fun __root___Foo_init_initialize(__kt: kotlin.native.internal.NativePtr, a: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    val __a = a
    kotlin.native.internal.initInstance(____kt, Foo(__a))
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__uintptr_t_float__")
public fun __root___Foo_init_initialize(__kt: kotlin.native.internal.NativePtr, f: Float): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_Foo_INSIDE_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_Foo_my_variable_set__TypesOfArguments__int64_t__")
public fun namespace_Foo_my_variable_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_NAMESPACED_CLASS_init_allocate")
public fun namespace_NAMESPACED_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.NAMESPACED_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_NAMESPACED_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_NAMESPACED_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, namespace.NAMESPACED_CLASS())
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

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set__TypesOfArguments__int64_t__")
public fun namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
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

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Foo_INSIDE_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_deeper_Foo_INSIDE_CLASS_my_variable_set__TypesOfArguments__int64_t__")
public fun namespace_deeper_Foo_INSIDE_CLASS_my_variable_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
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

@ExportedBridge("namespace_deeper_Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_deeper_Foo_my_variable_set__TypesOfArguments__int64_t__")
public fun namespace_deeper_Foo_my_variable_set(self: kotlin.native.internal.NativePtr, newValue: Long): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.deeper.Foo
    val __newValue = newValue
    __self.my_variable = __newValue
}

@ExportedBridge("namespace_deeper_NAMESPACED_CLASS_init_allocate")
public fun namespace_deeper_NAMESPACED_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.NAMESPACED_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_NAMESPACED_CLASS_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_NAMESPACED_CLASS_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize__TypesOfArguments__uintptr_t_int32_t__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize(__kt: kotlin.native.internal.NativePtr, i: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    val __i = i
    kotlin.native.internal.initInstance(____kt, namespace.deeper.OBJECT_WITH_PACKAGE.Bar(__i))
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.deeper.OBJECT_WITH_PACKAGE.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("namespace_deeper_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__int32_t__")
public fun namespace_deeper_OBJECT_WITH_PACKAGE_variable_set(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
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

@ExportedBridge("why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__uintptr_t__")
public fun why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

