@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main3BarC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Bar_p_get")
public fun Bar_p_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Bar
    val _result = run { __self.p }
    return _result
}

@ExportedBridge("__root___meaningOfLife")
public fun __root___meaningOfLife(): Boolean {
    val _result = run { meaningOfLife() }
    return _result
}

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Int32__")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Int32__(input: Int): Boolean {
    val __input = input
    val _result = run { meaningOfLife(__input) }
    return run { _result; true }
}

@ExportedBridge("__root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___(input: Boolean): kotlin.native.internal.NativePtr {
    val __input = run { input; null }
    val _result = run { meaningOfLife(__input) }
    return _result.objcPtr()
}

@ExportedBridge("__root___nothingClosure__TypesOfArguments__U2829202D_U20Swift_Never__")
public fun __root___nothingClosure__TypesOfArguments__U2829202D_U20Swift_Never__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(block);
        {
            val _result = kotlinFun()
            run { _result; throw IllegalStateException() }
        }
    }
    val _result = run { nothingClosure(__block) }
    return _result
}

@ExportedBridge("__root___nothingClosureParam__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_String__")
public fun __root___nothingClosureParam__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_String__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->kotlin.native.internal.NativePtr>(block);
        { arg0: Nothing ->
            val _result = kotlinFun(arg0)
            interpretObjCPointer<kotlin.String>(_result)
        }
    }
    val _result = run { nothingClosureParam(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___nothingFunctional")
public fun __root___nothingFunctional(): kotlin.native.internal.NativePtr {
    val _result = run { nothingFunctional() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nothingFunctionalParam")
public fun __root___nothingFunctionalParam(): kotlin.native.internal.NativePtr {
    val _result = run { nothingFunctionalParam() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nothingOptClosure__TypesOfArguments__U2829202D_U20Swift_Optional_Swift_Never___")
public fun __root___nothingOptClosure__TypesOfArguments__U2829202D_U20Swift_Optional_Swift_Never___(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Boolean>(block);
        {
            val _result = kotlinFun()
            run { _result; null }
        }
    }
    val _result = run { nothingOptClosure(__block) }
    return _result
}

@ExportedBridge("__root___nothingOptClosureParam__TypesOfArguments__U28Swift_Optional_Swift_Never_U29202D_U20Swift_String__")
public fun __root___nothingOptClosureParam__TypesOfArguments__U28Swift_Optional_Swift_Never_U29202D_U20Swift_String__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Boolean)->kotlin.native.internal.NativePtr>(block);
        { arg0: Nothing? ->
            val _result = kotlinFun(run { arg0; true })
            interpretObjCPointer<kotlin.String>(_result)
        }
    }
    val _result = run { nothingOptClosureParam(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___nothingOptFunctional")
public fun __root___nothingOptFunctional(): kotlin.native.internal.NativePtr {
    val _result = run { nothingOptFunctional() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nothingOptFunctionalParam")
public fun __root___nothingOptFunctionalParam(): kotlin.native.internal.NativePtr {
    val _result = run { nothingOptFunctionalParam() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___(input: Boolean): Boolean {
    val __input = run { input; null }
    val _result = run { nullableNothingInput(__input) }
    return run { _result; true }
}

@ExportedBridge("__root___nullableNothingOutput")
public fun __root___nullableNothingOutput(): Boolean {
    val _result = run { nullableNothingOutput() }
    return run { _result; true }
}

@ExportedBridge("__root___nullableNothingVariable_get")
public fun __root___nullableNothingVariable_get(): Boolean {
    val _result = run { nullableNothingVariable }
    return run { _result; true }
}

@ExportedBridge("__root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___")
public fun __root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___(newValue: Boolean): Boolean {
    val __newValue = run { newValue; null }
    val _result = run { nullableNothingVariable = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___value_get")
public fun __root___value_get(): Boolean {
    val _result = run { value }
    return _result
}

@ExportedBridge("__root___variable_get")
public fun __root___variable_get(): Boolean {
    val _result = run { variable }
    return _result
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun main_internal_functional_type_caller_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Nothing>).invoke() }
    return _result
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun main_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = run { (__pointerToBlock as Function0<Nothing?>).invoke() }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___")
public fun main_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___(pointerToBlock: kotlin.native.internal.NativePtr, _1: Boolean): kotlin.native.internal.NativePtr {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = run { _1; null }
    val _result = run { (__pointerToBlock as Function1<Nothing?, kotlin.String>).invoke(___1) }
    return _result.objcPtr()
}
