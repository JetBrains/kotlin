@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Foo_accept__TypesOfArguments__main_Bar_opt___")
public fun Foo_accept__TypesOfArguments__main_Bar_opt___(self: kotlin.native.internal.NativePtr, b: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __b = if (b == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(b) as Bar
    __self.accept(__b)
}

@ExportedBridge("Foo_any_value_get")
public fun Foo_any_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.any_value
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_produce")
public fun Foo_produce(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.produce()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_value_get")
public fun Foo_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.value
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_variable_get")
public fun Foo_variable_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val _result = __self.variable
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_variable_set__TypesOfArguments__main_Bar_opt___")
public fun Foo_variable_set__TypesOfArguments__main_Bar_opt___(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Bar
    __self.variable = __newValue
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

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt_main_Bar_opt___")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_main_Bar_opt___(__kt: kotlin.native.internal.NativePtr, b: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __b = if (b == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(b) as Bar
    kotlin.native.internal.initInstance(____kt, Foo(__b))
}

@ExportedBridge("__root___foo__TypesOfArguments__main_Bar__")
public fun __root___foo__TypesOfArguments__main_Bar__(a: kotlin.native.internal.NativePtr): Unit {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as Bar
    foo(__a)
}

@ExportedBridge("__root___foo__TypesOfArguments__main_Bar_opt___")
public fun __root___foo__TypesOfArguments__main_Bar_opt___(a: kotlin.native.internal.NativePtr): Unit {
    val __a = if (a == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(a) as Bar
    foo(__a)
}

@ExportedBridge("__root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase__(a: kotlin.native.internal.NativePtr): Unit {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as kotlin.Any
    foo_any(__a)
}

@ExportedBridge("__root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase_opt___")
public fun __root___foo_any__TypesOfArguments__KotlinRuntime_KotlinBase_opt___(a: kotlin.native.internal.NativePtr): Unit {
    val __a = if (a == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(a) as kotlin.Any
    foo_any(__a)
}

@ExportedBridge("__root___opt_to_non_opt_usage__TypesOfArguments__main_Bar_opt___")
public fun __root___opt_to_non_opt_usage__TypesOfArguments__main_Bar_opt___(i: kotlin.native.internal.NativePtr): Unit {
    val __i = if (i == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(i) as Bar
    opt_to_non_opt_usage(__i)
}

@ExportedBridge("__root___p")
public fun __root___p(): kotlin.native.internal.NativePtr {
    val _result = p()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___p_any")
public fun __root___p_any(): kotlin.native.internal.NativePtr {
    val _result = p_any()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___p_opt_opt_in__TypesOfArguments__main_Bar_opt___")
public fun __root___p_opt_opt_in__TypesOfArguments__main_Bar_opt___(input: kotlin.native.internal.NativePtr): Unit {
    val __input = if (input == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(input) as Bar
    p_opt_opt_in(__input)
}

@ExportedBridge("__root___p_opt_opt_out")
public fun __root___p_opt_opt_out(): kotlin.native.internal.NativePtr {
    val _result = p_opt_opt_out()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___primitive_get")
public fun __root___primitive_get(): kotlin.native.internal.NativePtr {
    val _result = primitive
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___primitive_in__TypesOfArguments__Swift_Bool_opt__Swift_Int8_opt__Swift_Int16_opt__Swift_Int32_opt__Swift_Int64_opt__Swift_UInt8_opt__Swift_UInt16_opt__Swift_UInt32_opt__Swift_UInt64_opt__Swift_Float_opt__Swift_Double_opt__Swift_Unicode_UTF16_CodeUnit_opt___")
public fun __root___primitive_in__TypesOfArguments__Swift_Bool_opt__Swift_Int8_opt__Swift_Int16_opt__Swift_Int32_opt__Swift_Int64_opt__Swift_UInt8_opt__Swift_UInt16_opt__Swift_UInt32_opt__Swift_UInt64_opt__Swift_Float_opt__Swift_Double_opt__Swift_Unicode_UTF16_CodeUnit_opt___(arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr, arg3: kotlin.native.internal.NativePtr, arg4: kotlin.native.internal.NativePtr, arg5: kotlin.native.internal.NativePtr, arg6: kotlin.native.internal.NativePtr, arg7: kotlin.native.internal.NativePtr, arg8: kotlin.native.internal.NativePtr, arg9: kotlin.native.internal.NativePtr, arg10: kotlin.native.internal.NativePtr, arg11: kotlin.native.internal.NativePtr, arg12: kotlin.native.internal.NativePtr): Unit {
    val __arg1 = if (arg1 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Boolean>(arg1)
    val __arg2 = if (arg2 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Byte>(arg2)
    val __arg3 = if (arg3 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Short>(arg3)
    val __arg4 = if (arg4 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(arg4)
    val __arg5 = if (arg5 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Long>(arg5)
    val __arg6 = if (arg6 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<UByte>(arg6)
    val __arg7 = if (arg7 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<UShort>(arg7)
    val __arg8 = if (arg8 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<UInt>(arg8)
    val __arg9 = if (arg9 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<ULong>(arg9)
    val __arg10 = if (arg10 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Float>(arg10)
    val __arg11 = if (arg11 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Double>(arg11)
    val __arg12 = if (arg12 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Char>(arg12)
    primitive_in(__arg1, __arg2, __arg3, __arg4, __arg5, __arg6, __arg7, __arg8, __arg9, __arg10, __arg11, __arg12)
}

@ExportedBridge("__root___primitive_out")
public fun __root___primitive_out(): kotlin.native.internal.NativePtr {
    val _result = primitive_out()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___primitive_set__TypesOfArguments__Swift_Double_opt___")
public fun __root___primitive_set__TypesOfArguments__Swift_Double_opt___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Double>(newValue)
    primitive = __newValue
}

@ExportedBridge("__root___str_get")
public fun __root___str_get(): kotlin.native.internal.NativePtr {
    val _result = str
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___str_set__TypesOfArguments__Swift_String_opt___")
public fun __root___str_set__TypesOfArguments__Swift_String_opt___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(newValue)
    str = __newValue
}

@ExportedBridge("__root___string_in__TypesOfArguments__Swift_String_opt___")
public fun __root___string_in__TypesOfArguments__Swift_String_opt___(a: kotlin.native.internal.NativePtr): Unit {
    val __a = if (a == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(a)
    string_in(__a)
}

@ExportedBridge("__root___string_out")
public fun __root___string_out(): kotlin.native.internal.NativePtr {
    val _result = string_out()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

