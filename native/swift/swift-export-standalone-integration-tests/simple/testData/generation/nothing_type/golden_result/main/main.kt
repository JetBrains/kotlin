@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Bar_p_get")
public fun Bar_p_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = run { __self.p }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___meaningOfLife")
public fun __root___meaningOfLife(): kotlin.native.internal.NativePtr {
    val _result = run { meaningOfLife() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Int32__")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Int32__(input: Int): Boolean {
    val __input = input
    val _result = run { meaningOfLife(__input) }
    return true
}

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___(input: Boolean): kotlin.native.internal.NativePtr {
    val __input = null
    val _result = run { meaningOfLife(__input) }
    return _result.objcPtr()
}

@ExportedBridge("__root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___(input: Boolean): Boolean {
    val __input = null
    val _result = run { nullableNothingInput(__input) }
    return run { _result; true }
}

@ExportedBridge("__root___nullableNothingOutput")
public fun __root___nullableNothingOutput(): Boolean {
    val _result = run { nullableNothingOutput() }
    return true
}

@ExportedBridge("__root___nullableNothingVariable_get")
public fun __root___nullableNothingVariable_get(): Boolean {
    val _result = run { nullableNothingVariable }
    return true
}

@ExportedBridge("__root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___(newValue: Boolean): Boolean {
    val __newValue = null
    val _result = run { nullableNothingVariable = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___value_get")
public fun __root___value_get(): kotlin.native.internal.NativePtr {
    val _result = run { value }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___variable_get")
public fun __root___variable_get(): kotlin.native.internal.NativePtr {
    val _result = run { variable }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
