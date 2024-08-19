import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@kotlinx.cinterop.internal.CCall("SwiftExport_ExportedKotlinPackages_dependency_four_AnotherBar_toRetainedSwift")
@kotlin.native.internal.ref.ToRetainedSwift(dependency.four.AnotherBar::class)
external fun SwiftExport_ExportedKotlinPackages_dependency_four_AnotherBar_toRetainedSwift(ref: kotlin.native.internal.ref.ExternalRCRef): kotlin.native.internal.NativePtr

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

