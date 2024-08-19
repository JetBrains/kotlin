import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@kotlinx.cinterop.internal.CCall("SwiftExport_main_Outer_toRetainedSwift")
@kotlin.native.internal.ref.ToRetainedSwift(Outer::class)
external fun SwiftExport_main_Outer_toRetainedSwift(ref: kotlin.native.internal.ref.ExternalRCRef): kotlin.native.internal.NativePtr

@ExportedBridge("__root___Outer_init_allocate")
public fun __root___Outer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Outer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Outer_init_initialize__TypesOfArguments__uintptr_t__")
public fun __root___Outer_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Outer())
}

