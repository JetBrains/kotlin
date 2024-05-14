import kotlin.native.internal.ExportedBridge

@ExportedBridge("typealiases_Foo_init_allocate")
public fun typealiases_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<typealiases.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun typealiases_Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, typealiases.Foo())
}

@ExportedBridge("typealiases_inner_Bar_init_allocate")
public fun typealiases_inner_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<typealiases.inner.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_inner_Bar_init_initialize__TypesOfArguments__uintptr_t__")
public fun typealiases_inner_Bar_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, typealiases.inner.Bar())
}

