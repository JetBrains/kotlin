@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(a.b.c.E::class, "22ExportedKotlinPackages1aO1bO1cO4mainE1EC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo.Nested::class, "4main3FooC6NestedC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Foo_Nested_init_allocate")
public fun Foo_Nested_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo.Nested>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_Nested_init_initialize__TypesOfArguments__Swift_UInt__")
public fun Foo_Nested_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo.Nested())
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo())
}

@ExportedBridge("a_b_c_E_A_get")
public fun a_b_c_E_A_get(): kotlin.native.internal.NativePtr {
    val _result = a.b.c.E.A
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("a_b_c_E_B_get")
public fun a_b_c_E_B_get(): kotlin.native.internal.NativePtr {
    val _result = a.b.c.E.B
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("a_b_c_E_C_get")
public fun a_b_c_E_C_get(): kotlin.native.internal.NativePtr {
    val _result = a.b.c.E.C
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("a_b_c_E_entries_get")
public fun a_b_c_E_entries_get(): kotlin.native.internal.NativePtr {
    val _result = a.b.c.E.entries
    return _result.objcPtr()
}

@ExportedBridge("a_b_c_E_valueOf__TypesOfArguments__Swift_String__")
public fun a_b_c_E_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = a.b.c.E.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

