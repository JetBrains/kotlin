@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

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

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Int32__")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Int32__(input: Int): Unit {
    val __input = input
    val _result = meaningOfLife(__input)
    return Unit
}

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___(): kotlin.native.internal.NativePtr {
    val __input = null
    val _result = meaningOfLife(__input)
    return _result.objcPtr()
}

@ExportedBridge("__root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___(): Unit {
    val __input = null
    nullableNothingInput(__input)
}

@ExportedBridge("__root___nullableNothingOutput")
public fun __root___nullableNothingOutput(): Unit {
    val _result = nullableNothingOutput()
    return Unit
}

@ExportedBridge("__root___nullableNothingVariable_get")
public fun __root___nullableNothingVariable_get(): Unit {
    val _result = nullableNothingVariable
    return Unit
}

@ExportedBridge("__root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___(): Unit {
    val __newValue = null
    nullableNothingVariable = __newValue
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

