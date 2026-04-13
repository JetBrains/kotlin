@file:OptIn(InternalLibApi::class, MyOptInApi::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyImplementation::class, "4main16MyImplementationC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyOptInClass::class, "4main12MyOptInClassC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("MyImplementation_bar")
@OptIn(InternalLibApi::class)
public fun MyImplementation_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyImplementation
    val _result = run { __self.bar() }
    return run { _result; true }
}

@ExportedBridge("MyImplementation_foo_get")
@OptIn(InternalLibApi::class)
public fun MyImplementation_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyImplementation
    val _result = run { __self.foo }
    return _result.objcPtr()
}

@ExportedBridge("MyImplementation_foo_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun MyImplementation_foo_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyImplementation
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___MyImplementation_init_allocate")
@OptIn(InternalLibApi::class)
public fun __root___MyImplementation_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<MyImplementation>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MyImplementation_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(InternalLibApi::class)
public fun __root___MyImplementation_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, MyImplementation()) }
    return run { _result; true }
}

@ExportedBridge("__root___MyOptInClass_init_allocate")
@OptIn(MyOptInApi::class)
public fun __root___MyOptInClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<MyOptInClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MyOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(MyOptInApi::class)
public fun __root___MyOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, MyOptInClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___callbackFunction__TypesOfArguments__U2829202D_U20main_MyOptInClass__")
@OptIn(MyOptInApi::class)
public fun __root___callbackFunction__TypesOfArguments__U2829202D_U20main_MyOptInClass__(action: kotlin.native.internal.NativePtr): Boolean {
    val __action = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->kotlin.native.internal.NativePtr>(action);
        {
            val _result = kotlinFun()
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as MyOptInClass
        }
    }
    val _result = run { callbackFunction(__action) }
    return run { _result; true }
}

@ExportedBridge("__root___functionalTypePropertyA_get")
@OptIn(MyOptInApi::class)
public fun __root___functionalTypePropertyA_get(): kotlin.native.internal.NativePtr {
    val _result = run { functionalTypePropertyA }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___functionalTypePropertyA_set__TypesOfArguments__U28main_MyOptInClassU29202D_U20Swift_Void__")
@OptIn(MyOptInApi::class)
public fun __root___functionalTypePropertyA_set__TypesOfArguments__U28main_MyOptInClassU29202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(newValue);
        { arg0: MyOptInClass ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            run<Unit> { _result }
        }
    }
    val _result = run { functionalTypePropertyA = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___functionalTypePropertyB_get")
@OptIn(InternalLibApi::class)
public fun __root___functionalTypePropertyB_get(): kotlin.native.internal.NativePtr {
    val _result = run { functionalTypePropertyB }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___functionalTypePropertyB_set__TypesOfArguments__U28anyU20lib_InternalLibInterfaceU29202D_U20Swift_Void__")
@OptIn(InternalLibApi::class)
public fun __root___functionalTypePropertyB_set__TypesOfArguments__U28anyU20lib_InternalLibInterfaceU29202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(newValue);
        { arg0: InternalLibInterface ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            run<Unit> { _result }
        }
    }
    val _result = run { functionalTypePropertyB = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___optInFunctionA")
@OptIn(MyOptInApi::class)
public fun __root___optInFunctionA(): Boolean {
    val _result = run { optInFunctionA() }
    return run { _result; true }
}

@ExportedBridge("__root___optInFunctionB")
@OptIn(ExperimentalLibApi::class)
public fun __root___optInFunctionB(): Boolean {
    val _result = run { optInFunctionB() }
    return run { _result; true }
}

@ExportedBridge("__root___optInFunctionC")
@OptIn(ExperimentalLibApi::class)
public fun __root___optInFunctionC(): kotlin.native.internal.NativePtr {
    val _result = run { optInFunctionC() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___optInFunctionD")
@OptIn(InternalLibApi::class)
public fun __root___optInFunctionD(): kotlin.native.internal.NativePtr {
    val _result = run { optInFunctionD() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___optInFunctionE")
@OptIn(InternalLibApi::class)
public fun __root___optInFunctionE(): kotlin.native.internal.NativePtr {
    val _result = run { optInFunctionE() }
    return _result.objcPtr()
}

@ExportedBridge("__root___optInFunctionF")
@OptIn(MyOptInApi::class)
public fun __root___optInFunctionF(): Boolean {
    val _result = run { optInFunctionF() }
    return run { _result; true }
}

@ExportedBridge("__root___regularFunctionA")
public fun __root___regularFunctionA(): Boolean {
    val _result = run { regularFunctionA() }
    return run { _result; true }
}

@ExportedBridge("__root___regularFunctionB")
public fun __root___regularFunctionB(): Boolean {
    val _result = run { regularFunctionB() }
    return run { _result; true }
}

@ExportedBridge("__root___regularFunctionC")
public fun __root___regularFunctionC(): kotlin.native.internal.NativePtr {
    val _result = run { regularFunctionC() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_MyOptInClass__")
@OptIn(MyOptInApi::class)
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_MyOptInClass__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as MyOptInClass
    val _result = run { (__pointerToBlock as Function1<MyOptInClass, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20lib_InternalLibInterface__")
@OptIn(InternalLibApi::class)
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20lib_InternalLibInterface__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as InternalLibInterface
    val _result = run { (__pointerToBlock as Function1<InternalLibInterface, Unit>).invoke(___1) }
    return run { _result; true }
}
