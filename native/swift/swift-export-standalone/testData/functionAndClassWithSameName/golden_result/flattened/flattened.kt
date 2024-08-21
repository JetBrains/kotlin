import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("flattenedPackage_FlattenedPackageClass__TypesOfArguments__int32_t__")
public fun flattenedPackage_FlattenedPackageClass(i: Int): kotlin.native.internal.NativePtr {
    val __i = i
    val _result = flattenedPackage.FlattenedPackageClass(__i)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("flattenedPackage_FlattenedPackageClass_init_allocate")
public fun flattenedPackage_FlattenedPackageClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<flattenedPackage.FlattenedPackageClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__uintptr_t__")
public fun flattenedPackage_FlattenedPackageClass_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, flattenedPackage.FlattenedPackageClass())
}
