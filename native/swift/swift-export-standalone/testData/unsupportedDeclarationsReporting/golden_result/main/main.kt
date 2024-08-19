import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Foo_Nested_init_allocate")
public fun Foo_Nested_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo.Nested>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_Nested_init_initialize__TypesOfArguments__uintptr_t__")
public fun Foo_Nested_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo.Nested())
}

@kotlinx.cinterop.internal.CCall("SwiftExport_main_Foo_toRetainedSwift")
@kotlin.native.internal.ref.ToRetainedSwift(Foo::class)
external fun SwiftExport_main_Foo_toRetainedSwift(ref: kotlin.native.internal.ref.ExternalRCRef): kotlin.native.internal.NativePtr

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

