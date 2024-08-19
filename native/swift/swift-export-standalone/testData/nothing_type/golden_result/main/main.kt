import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Bar_p_get")
public fun Bar_p_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = __self.p
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@kotlinx.cinterop.internal.CCall("SwiftExport_main_Bar_toRetainedSwift")
@kotlin.native.internal.ref.ToRetainedSwift(Bar::class)
external fun SwiftExport_main_Bar_toRetainedSwift(ref: kotlin.native.internal.ref.ExternalRCRef): kotlin.native.internal.NativePtr

@ExportedBridge("__root___meaningOfLife")
public fun __root___meaningOfLife(): kotlin.native.internal.NativePtr {
    val _result = meaningOfLife()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___value_get")
public fun __root___value_get(): kotlin.native.internal.NativePtr {
    val _result = value
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___variable_get")
public fun __root___variable_get(): kotlin.native.internal.NativePtr {
    val _result = variable
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

