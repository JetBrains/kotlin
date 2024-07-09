import kotlin.native.internal.ExportedBridge

@ExportedBridge("dependency_four_AnotherBar_init_allocate")
public fun dependency_four_AnotherBar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<dependency.four.AnotherBar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("dependency_four_AnotherBar_init_initialize__TypesOfArguments__uintptr_t__")
public fun dependency_four_AnotherBar_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, dependency.four.AnotherBar())
}
