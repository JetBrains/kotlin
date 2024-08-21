import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("test_factory_modules_ClassFromDependency_init_allocate")
public fun test_factory_modules_ClassFromDependency_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<test.factory.modules.ClassFromDependency>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_modules_ClassFromDependency_init_initialize__TypesOfArguments__uintptr_t__")
public fun test_factory_modules_ClassFromDependency_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, test.factory.modules.ClassFromDependency())
}
