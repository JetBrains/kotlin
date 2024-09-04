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
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Bar())
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt_main_Bar_opt___")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_main_Bar_opt___(__kt: kotlin.native.internal.NativePtr, b: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
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

@ExportedBridge("__root___str_get")
public fun __root___str_get(): kotlin.native.internal.NativePtr {
    val _result = str
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___str_set__TypesOfArguments__Swift_String_opt___")
public fun __root___str_set__TypesOfArguments__Swift_String_opt___(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<String>(newValue)
    str = __newValue
}

@ExportedBridge("__root___string_in__TypesOfArguments__Swift_String_opt___")
public fun __root___string_in__TypesOfArguments__Swift_String_opt___(a: kotlin.native.internal.NativePtr): Unit {
    val __a = if (a == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<String>(a)
    string_in(__a)
}

@ExportedBridge("__root___string_out")
public fun __root___string_out(): kotlin.native.internal.NativePtr {
    val _result = string_out()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

