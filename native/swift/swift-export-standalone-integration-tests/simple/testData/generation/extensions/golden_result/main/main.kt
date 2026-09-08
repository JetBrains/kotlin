@file:OptIn(ExperimentalApi::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(foo.Foo::class, "_ExportedKotlinPackages_foo_Foo")
@file:kotlin.native.internal.objc.BindClassToObjCName(other.Other::class, "22ExportedKotlinPackages5otherO4mainE5OtherC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Baz::class, "4main3BazC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DeprecatedBar::class, "4main13DeprecatedBarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GenericClass::class, "4main12GenericClassC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import foo.contextExtFun as foo_contextExtFun
import foo.contextProp as foo_contextProp
import foo.nullableFun as foo_nullableFun
import foo.nullableProp as foo_nullableProp
import foo.simpleExtFun as foo_simpleExtFun
import foo.simpleExtFunWithArgs as foo_simpleExtFunWithArgs
import foo.simplePropVar as foo_simplePropVar
import foo.simpleProp as foo_simpleProp
import foo.varargExtFun as foo_varargExtFun
import other.otherExtFun as other_otherExtFun
import other.otherProp as other_otherProp

@ImportedBridge("foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse_swift")
internal external fun foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse_swift(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(foo.Foo::class, "doubleReceiverExtFun")
public fun foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse(self: foo.Foo, `receiver`: Bar): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __receiver = kotlin.native.internal.ref.createRetainedExternalRCRef(`receiver`)
    val _result = foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse_swift(__self, __receiver)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ExportedBridge("Bar_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
@OptIn(ExperimentalApi::class)
public fun Bar_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __self.run { __receiver.doubleReceiverExtFun() } }
    return _result.objcPtr()
}

@ExportedBridge("Bar_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
@OptIn(ExperimentalApi::class)
public fun Bar_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __self.run { __receiver.doubleReceiverExtProp } }
    return _result
}

@ExportedBridge("Baz_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
public fun Baz_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __self.run { __receiver.doubleReceiverExtFun() } }
    return _result.objcPtr()
}

@ExportedBridge("Baz_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
public fun Baz_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __self.run { __receiver.doubleReceiverExtProp } }
    return _result
}

@ExportedBridge("__root___Bar_init_allocate")
@OptIn(ExperimentalApi::class)
public fun __root___Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Bar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(ExperimentalApi::class)
public fun __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Bar()) }
    return run { _result; true }
}

@ExportedBridge("__root___Baz_get")
public fun __root___Baz_get(): kotlin.native.internal.NativePtr {
    val _result = run { Baz }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DeprecatedBar_init_allocate")
public fun __root___DeprecatedBar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<DeprecatedBar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DeprecatedBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___DeprecatedBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, DeprecatedBar()) }
    return run { _result; true }
}

@ExportedBridge("__root___GenericClass_init_allocate")
public fun __root___GenericClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<GenericClass<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GenericClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___GenericClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, GenericClass<kotlin.Any?>()) }
    return run { _result; true }
}

@ExportedBridge("__root___deprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__")
@OptIn(ExperimentalApi::class)
public fun __root___deprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val _result = run { __receiver.deprecatedSetterProp }
    return _result
}

@ExportedBridge("__root___deprecatedSetterProp_set__TypesOfArgumentsE__main_Bar_Swift_Bool__")
@OptIn(ExperimentalApi::class)
public fun __root___deprecatedSetterProp_set__TypesOfArgumentsE__main_Bar_Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, value: Boolean): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val __value = value
    val _result = run { __receiver.deprecatedSetterProp = __value }
    return run { _result; true }
}

@ExportedBridge("__root___errorDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__")
@OptIn(ExperimentalApi::class)
public fun __root___errorDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val _result = run { __receiver.errorDeprecatedSetterProp }
    return _result
}

@ExportedBridge("__root___funExtFun__TypesOfArgumentsE__U2829202D_U20Swift_Void__")
public fun __root___funExtFun__TypesOfArgumentsE__U2829202D_U20Swift_Void__(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(`receiver`);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { __receiver.funExtFun() }
    return _result
}

@ExportedBridge("__root___funExtProp_get__TypesOfArgumentsE__U2829202D_U20Swift_Void__")
public fun __root___funExtProp_get__TypesOfArgumentsE__U2829202D_U20Swift_Void__(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(`receiver`);
        {
            val _result = kotlinFun()
            run<Unit> { _result }
        }
    }
    val _result = run { __receiver.funExtProp }
    return _result
}

@ExportedBridge("__root___genericExtFun__TypesOfArgumentsE__main_GenericClass__")
public fun __root___genericExtFun__TypesOfArgumentsE__main_GenericClass__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as GenericClass<kotlin.Any?>
    val _result = run { __receiver.genericExtFun() }
    return _result.objcPtr()
}

@ExportedBridge("__root___genericExtProp_get__TypesOfArgumentsE__main_GenericClass__")
public fun __root___genericExtProp_get__TypesOfArgumentsE__main_GenericClass__(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as GenericClass<kotlin.Any?>
    val _result = run { __receiver.genericExtProp }
    return _result
}

@ExportedBridge("__root___genericUpperBoundExtFun__TypesOfArgumentsE__main_GenericClass__")
public fun __root___genericUpperBoundExtFun__TypesOfArgumentsE__main_GenericClass__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as GenericClass<kotlin.Any?>
    val _result = run { __receiver.genericUpperBoundExtFun() }
    return _result.objcPtr()
}

@ExportedBridge("__root___genericUpperBoundExtProp_get__TypesOfArgumentsE__main_GenericClass__")
public fun __root___genericUpperBoundExtProp_get__TypesOfArgumentsE__main_GenericClass__(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as GenericClass<kotlin.Any?>
    val _result = run { __receiver.genericUpperBoundExtProp }
    return _result
}

@ExportedBridge("__root___hiddenDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__")
@OptIn(ExperimentalApi::class)
public fun __root___hiddenDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val _result = run { __receiver.hiddenDeprecatedSetterProp }
    return _result
}

@ExportedBridge("__root___optInExtFun__TypesOfArgumentsE__main_Baz__")
@OptIn(ExperimentalApi::class)
public fun __root___optInExtFun__TypesOfArgumentsE__main_Baz__(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Baz
    val _result = run { __receiver.optInExtFun() }
    return _result
}

@ExportedBridge("__root___optInProp_get__TypesOfArgumentsE__main_Baz__")
@OptIn(ExperimentalApi::class)
public fun __root___optInProp_get__TypesOfArgumentsE__main_Baz__(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Baz
    val _result = run { __receiver.optInProp }
    return _result
}

@ExportedBridge("__root___optInProp_set__TypesOfArgumentsE__main_Baz_Swift_Int32__")
@OptIn(ExperimentalApi::class)
public fun __root___optInProp_set__TypesOfArgumentsE__main_Baz_Swift_Int32__(`receiver`: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Baz
    val __value = value
    val _result = run { __receiver.optInProp = __value }
    return run { _result; true }
}

@ExportedBridge("__root___optInSetterProp_get__TypesOfArgumentsE__main_Baz__")
public fun __root___optInSetterProp_get__TypesOfArgumentsE__main_Baz__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Baz
    val _result = run { __receiver.optInSetterProp }
    return _result.objcPtr()
}

@ExportedBridge("__root___optInSetterProp_set__TypesOfArgumentsE__main_Baz_Swift_String__")
@OptIn(ExperimentalApi::class)
public fun __root___optInSetterProp_set__TypesOfArgumentsE__main_Baz_Swift_String__(`receiver`: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Baz
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { __receiver.optInSetterProp = __value }
    return run { _result; true }
}

@ExportedBridge("foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as foo.Foo
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val _result = run { __self.run { __receiver.doubleReceiverExtFun() } }
    return _result.objcPtr()
}

@ExportedBridge("foo_Foo_doubleReceiverExtProp_get__TypesOfArgumentsE__main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_Foo_doubleReceiverExtProp_get__TypesOfArgumentsE__main_Bar__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as foo.Foo
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Bar
    val _result = run { __self.run { __receiver.doubleReceiverExtProp } }
    return _result
}

@ExportedBridge("foo_contextExtFun__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Bool_main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_contextExtFun__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Bool_main_Bar__(`receiver`: kotlin.native.internal.NativePtr, arg: Boolean, bar: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val __arg = arg
    val __bar = kotlin.native.internal.ref.dereferenceExternalRCRef(bar) as Bar
    val _result = run { context(__bar) { __receiver.foo_contextExtFun(__arg) } }
    return _result
}

@ExportedBridge("foo_contextProp_get__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_contextProp_get__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_main_Bar__(`receiver`: kotlin.native.internal.NativePtr, bar: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val __bar = kotlin.native.internal.ref.dereferenceExternalRCRef(bar) as Bar
    val _result = run { context(__bar) { __receiver.foo_contextProp } }
    return _result
}

@ExportedBridge("foo_contextProp_set__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_contextProp_set__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__(`receiver`: kotlin.native.internal.NativePtr, `_`: Int, bar: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val `___` = `_`
    val __bar = kotlin.native.internal.ref.dereferenceExternalRCRef(bar) as Bar
    val _result = run { context(__bar) { __receiver.foo_contextProp = `___` } }
    return run { _result; true }
}

@ExportedBridge("foo_nullableFun__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___")
public fun foo_nullableFun__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __receiver.foo_nullableFun() }
    return _result
}

@ExportedBridge("foo_nullableProp_get__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___")
public fun foo_nullableProp_get__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___(`receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __receiver.foo_nullableProp }
    return _result
}

@ExportedBridge("foo_nullableProp_set__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo__Swift_Bool__")
public fun foo_nullableProp_set__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo__Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, `_`: Boolean): Boolean {
    val __receiver = if (`receiver` == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val `___` = `_`
    val _result = run { __receiver.foo_nullableProp = `___` }
    return run { _result; true }
}

@ExportedBridge("foo_simpleExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
public fun foo_simpleExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __receiver.foo_simpleExtFun() }
    return _result.objcPtr()
}

@ExportedBridge("foo_simpleExtFunWithArgs__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__")
@OptIn(ExperimentalApi::class)
public fun foo_simpleExtFunWithArgs__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__(`receiver`: kotlin.native.internal.NativePtr, arg1: Int, arg2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val __arg1 = arg1
    val __arg2 = kotlin.native.internal.ref.dereferenceExternalRCRef(arg2) as Bar
    val _result = run { __receiver.foo_simpleExtFunWithArgs(__arg1, __arg2) }
    return _result.objcPtr()
}

@ExportedBridge("foo_simplePropVar_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
public fun foo_simplePropVar_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __receiver.foo_simplePropVar }
    return _result.objcPtr()
}

@ExportedBridge("foo_simplePropVar_set__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_String__")
public fun foo_simplePropVar_set__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_String__(`receiver`: kotlin.native.internal.NativePtr, `_`: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val `___` = interpretObjCPointer<kotlin.String>(`_`)
    val _result = run { __receiver.foo_simplePropVar = `___` }
    return run { _result; true }
}

@ExportedBridge("foo_simpleProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__")
public fun foo_simpleProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val _result = run { __receiver.foo_simpleProp }
    return _result.objcPtr()
}

@ExportedBridge("foo_varargExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Array_Swift_String__Vararg___")
public fun foo_varargExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Array_Swift_String__Vararg___(`receiver`: kotlin.native.internal.NativePtr, args: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as foo.Foo
    val __args = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(args).toTypedArray()
    val _result = run { __receiver.foo_varargExtFun(*__args) }
    return _result
}

@ExportedBridge("other_Other_init_allocate")
public fun other_Other_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<other.Other>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("other_Other_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun other_Other_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, other.Other()) }
    return run { _result; true }
}

@ExportedBridge("other_otherExtFun__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__")
public fun other_otherExtFun__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as other.Other
    val _result = run { __receiver.other_otherExtFun() }
    return _result.objcPtr()
}

@ExportedBridge("other_otherProp_get__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__")
public fun other_otherProp_get__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as other.Other
    val _result = run { __receiver.other_otherProp }
    return _result
}

@ExportedBridge("other_otherProp_set__TypesOfArgumentsE__ExportedKotlinPackages_other_Other_Swift_Int32__")
public fun other_otherProp_set__TypesOfArgumentsE__ExportedKotlinPackages_other_Other_Swift_Int32__(`receiver`: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as other.Other
    val __value = value
    val _result = run { __receiver.other_otherProp = __value }
    return run { _result; true }
}
