@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Baz::class, "4main3BazC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "_main_Bar")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("Bar_bar_get__reverse_swift")
internal external fun Bar_bar_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Bar::class, "<get-bar>")
public fun Bar_bar_get__reverse(self: Bar): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Bar_bar_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Bar_bar_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Bar_bar_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Bar::class, "<set-bar>")
public fun Bar_bar_set__TypesOfArguments__Swift_String____reverse(self: Bar, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = Bar_bar_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("Bar_foo__reverse_swift")
internal external fun Bar_foo__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Bar::class, "foo")
public fun Bar_foo__reverse(self: Bar): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Bar_foo__reverse_swift(__self)
    return run<Unit> { _result }
}

@ExportedBridge("Bar_bar_get")
public fun Bar_bar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = run { __self.bar }
    return _result.objcPtr()
}

@ExportedBridge("Bar_bar_set__TypesOfArguments__Swift_String__")
public fun Bar_bar_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.bar = __newValue }
    return run { _result; true }
}

@ExportedBridge("Bar_foo")
public fun Bar_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = run { __self.foo() }
    return run { _result; true }
}

@ExportedBridge("Baz_someInternalLibFunction")
public fun Baz_someInternalLibFunction(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val _result = run { __self.someInternalLibFunction() }
    return run { _result; true }
}

@ExportedBridge("Baz_someNormalFunction")
public fun Baz_someNormalFunction(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val _result = run { __self.someNormalFunction() }
    return _result.objcPtr()
}

@ExportedBridge("Baz_someUndocumentedFunction")
public fun Baz_someUndocumentedFunction(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val _result = run { __self.someUndocumentedFunction() }
    return _result
}

@ExportedBridge("Foo_a_get")
public fun Foo_a_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = run { __self.a }
    return _result.objcPtr()
}

@ExportedBridge("Foo_b_get")
public fun Foo_b_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = run { __self.b }
    return _result
}

@ExportedBridge("Foo_bar__TypesOfArgumentsC2__Swift_String_Swift_Int32__")
public fun Foo_bar__TypesOfArgumentsC2__Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr, b: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __a = interpretObjCPointer<kotlin.String>(a)
    val __b = b
    val _result = run { context(__a, __b) { __self.bar() } }
    return _result
}

@ExportedBridge("Foo_baz__TypesOfArgumentsE__Swift_String__")
public fun Foo_baz__TypesOfArgumentsE__Swift_String__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val _result = run { __self.run { __receiver.baz() } }
    return _result
}

@ExportedBridge("Foo_d_get")
public fun Foo_d_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = run { __self.d }
    return _result
}

@ExportedBridge("Foo_d_set__TypesOfArguments__Swift_Bool__")
public fun Foo_d_set__TypesOfArguments__Swift_Bool__(self: kotlin.native.internal.NativePtr, newValue: Boolean): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __newValue = newValue
    val _result = run { __self.d = __newValue }
    return run { _result; true }
}

@ExportedBridge("Foo_e_get__TypesOfArgumentsC1__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun Foo_e_get__TypesOfArgumentsC1__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, c: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __c = kotlin.native.internal.ref.dereferenceExternalRCRef(c) as kotlin.Any
    val _result = run { context(__c) { __self.e } }
    return _result
}

@ExportedBridge("Foo_e_set__TypesOfArgumentsC1__Swift_Int32_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun Foo_e_set__TypesOfArgumentsC1__Swift_Int32_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, `_`: Int, c: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val `___` = `_`
    val __c = kotlin.native.internal.ref.dereferenceExternalRCRef(c) as kotlin.Any
    val _result = run { context(__c) { __self.e = `___` } }
    return run { _result; true }
}

@ExportedBridge("Foo_f_get")
public fun Foo_f_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = run { __self.f }
    return _result.objcPtr()
}

@ExportedBridge("Foo_foo__TypesOfArgumentsC1__Swift_Int32_Swift_String__")
public fun Foo_foo__TypesOfArgumentsC1__Swift_Int32_Swift_String__(self: kotlin.native.internal.NativePtr, c: Int, a: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __c = c
    val __a = interpretObjCPointer<kotlin.String>(a)
    val _result = run { context(__a) { __self.foo(__c) } }
    return run { _result; true }
}

@ExportedBridge("Foo_g_get__TypesOfArgumentsE__Swift_String__")
public fun Foo_g_get__TypesOfArgumentsE__Swift_String__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val _result = run { __self.run { __receiver.g } }
    return _result
}

@ExportedBridge("__root___Baz_get")
public fun __root___Baz_get(): kotlin.native.internal.NativePtr {
    val _result = run { Baz }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Foo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Int32_Swift_Bool__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Int32_Swift_Bool__(__kt: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr, z: Int, c: Boolean): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = interpretObjCPointer<kotlin.String>(a)
    val __z = z
    val __c = c
    val _result = run { kotlin.native.internal.initInstance(____kt, Foo(__a, __z, __c)) }
    return run { _result; true }
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Foo()) }
    return run { _result; true }
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(__kt: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = run { kotlin.native.internal.initInstance(____kt, Foo(__arg)) }
    return run { _result; true }
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = interpretObjCPointer<kotlin.String>(a)
    val _result = run { kotlin.native.internal.initInstance(____kt, Foo(__a)) }
    return run { _result; true }
}
