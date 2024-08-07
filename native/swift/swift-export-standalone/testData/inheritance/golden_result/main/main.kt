import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun __root___Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo())
}

@ExportedBridge("__root___foo_get")
public fun __root___foo_get(): kotlin.native.internal.NativePtr {
    val _result = foo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_set__TypesOfArguments__uintptr_t__")
public fun __root___foo_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foo
    foo = __newValue
}

@ExportedBridge("__root___getFoo")
public fun __root___getFoo(): kotlin.native.internal.NativePtr {
    val _result = getFoo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

