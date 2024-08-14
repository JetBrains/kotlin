import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("testData_kt_70067_kt_70067_ExampleClass_init_allocate")
public fun testData_kt_70067_kt_70067_ExampleClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<testData.kt_70067.kt_70067.ExampleClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("testData_kt_70067_kt_70067_ExampleClass_init_initialize__TypesOfArguments__uintptr_t__")
public fun testData_kt_70067_kt_70067_ExampleClass_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, testData.kt_70067.kt_70067.ExampleClass())
}

