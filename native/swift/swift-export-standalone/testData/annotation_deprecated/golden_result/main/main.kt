import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("org_kotlin_foo_ExampleClass_init_allocate")
public fun org_kotlin_foo_ExampleClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.ExampleClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_ExampleClass_init_initialize__TypesOfArguments__uintptr_t__")
public fun org_kotlin_foo_ExampleClass_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, org.kotlin.foo.ExampleClass())
}

