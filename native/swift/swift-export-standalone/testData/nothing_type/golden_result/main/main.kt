import kotlin.native.internal.ExportedBridge

@ExportedBridge("Bar_p_get")
public fun Bar_p_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = __self.p
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

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
