@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Context::class, "4main7ContextC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ContextA::class, "4main8ContextAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ContextB::class, "4main8ContextBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Foo_bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__")
public fun Foo_bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { __self.bar(__arg) } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_baz_get__TypesOfArguments__main_Context__")
public fun Foo_baz_get__TypesOfArguments__main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { __self.baz } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_complexContextFunction__TypesOfArguments__Swift_String_Swift_Int32_main_ContextA_main_Context_main_ContextB__")
public fun Foo_complexContextFunction__TypesOfArguments__Swift_String_Swift_Int32_main_ContextA_main_Context_main_ContextB__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, count: Int, contextA: kotlin.native.internal.NativePtr, context: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __count = count
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as Context
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = run { context(__contextA, __context, __contextB) { __self.run { __receiver.complexContextFunction(__count) } } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextB_main_ContextA__")
public fun Foo_complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextB_main_ContextA__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val _result = run { context(__contextB, __contextA) { __self.run { __receiver.complexContextProperty } } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Int32_main_ContextB_main_ContextA__")
public fun Foo_complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Int32_main_ContextB_main_ContextA__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, value: Int, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val _result = run { context(__contextB, __contextA) { __self.run { __receiver.complexContextProperty = __value } } }
    return run { _result; true }
}

@ExportedBridge("Foo_foo__TypesOfArguments__main_Context__")
public fun Foo_foo__TypesOfArguments__main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { __self.foo() } }
    return run { _result; true }
}

@ExportedBridge("Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__")
public fun Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(self: kotlin.native.internal.NativePtr, _0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as Context
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextB
    val _result = run { context(___0, __ctx) { __self.unnamedContextParametersFunction() } }
    return run { _result; true }
}

@ExportedBridge("Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__")
public fun Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as Context
    val _result = run { context(__ctx, ___1) { __self.unnamedContextParametersProperty } }
    return _result.objcPtr()
}

@ExportedBridge("Foo_unnamedContextParametersProperty_set__TypesOfArguments__Swift_String_main_ContextA_main_Context__")
public fun Foo_unnamedContextParametersProperty_set__TypesOfArguments__Swift_String_main_ContextA_main_Context__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _2: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __value = interpretObjCPointer<kotlin.String>(value)
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___2 = kotlin.native.internal.ref.dereferenceExternalRCRef(_2) as Context
    val _result = run { context(__ctx, ___2) { __self.unnamedContextParametersProperty = __value } }
    return run { _result; true }
}

@ExportedBridge("__root___ContextA_init_allocate")
public fun __root___ContextA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ContextA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ContextA()) }
    return run { _result; true }
}

@ExportedBridge("__root___ContextB_init_allocate")
public fun __root___ContextB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ContextB>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ContextB()) }
    return run { _result; true }
}

@ExportedBridge("__root___Context_init_allocate")
public fun __root___Context_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Context>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Context()) }
    return run { _result; true }
}

@ExportedBridge("__root___Foo_get")
public fun __root___Foo_get(): kotlin.native.internal.NativePtr {
    val _result = run { Foo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__")
public fun __root___bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__(arg: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { bar(__arg) } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz_get__TypesOfArguments__main_Context__")
public fun __root___baz_get__TypesOfArguments__main_Context__(ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { baz } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___complexContextFunction__TypesOfArguments__Swift_String_Swift_Bool_main_Context_main_ContextA_main_ContextB__")
public fun __root___complexContextFunction__TypesOfArguments__Swift_String_Swift_Bool_main_Context_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, yes: Boolean, context: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Int {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __yes = yes
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as Context
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = run { context(__context, __contextA, __contextB) { __receiver.complexContextFunction(__yes) } }
    return _result
}

@ExportedBridge("__root___complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextA_main_ContextB__")
public fun __root___complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = run { context(__contextA, __contextB) { __receiver.complexContextProperty } }
    return _result
}

@ExportedBridge("__root___complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Bool_main_ContextA_main_ContextB__")
public fun __root___complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Bool_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, value: Boolean, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = run { context(__contextA, __contextB) { __receiver.complexContextProperty = __value } }
    return run { _result; true }
}

@ExportedBridge("__root___contextBlockA__TypesOfArguments__U28Swift_Int32_U20Swift_StringU29202D_U20Swift_Void__")
public fun __root___contextBlockA__TypesOfArguments__U28Swift_Int32_U20Swift_StringU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr, Int, kotlin.native.internal.NativePtr)->Boolean>(block);
        { ctx0: ContextA, ctx1: ContextB, arg0: Int, arg1: kotlin.String ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(ctx0), kotlin.native.internal.ref.createRetainedExternalRCRef(ctx1), arg0, arg1.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { contextBlockA(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___contextBlockB")
public fun __root___contextBlockB(): kotlin.native.internal.NativePtr {
    val _result = run { contextBlockB() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___contextBlockC__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void__")
public fun __root___contextBlockC__TypesOfArguments__U28Swift_StringU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->Boolean>(block);
        { ctx0: Context, arg0: kotlin.String ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(ctx0), arg0.objcPtr())
            run<Unit> { _result }
        }
    }
    val _result = run { contextBlockC(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___contextBlockD")
public fun __root___contextBlockD(): kotlin.native.internal.NativePtr {
    val _result = run { contextBlockD() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo__TypesOfArguments__main_Context__")
public fun __root___foo__TypesOfArguments__main_Context__(ctx: kotlin.native.internal.NativePtr): Boolean {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(__ctx) { foo() } }
    return run { _result; true }
}

@ExportedBridge("__root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__")
public fun __root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as ContextB
    val _result = run { context(__ctx, ___1) { unnamedContextParametersFunction() } }
    return run { _result; true }
}

@ExportedBridge("__root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__")
public fun __root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(_0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Int {
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(___0, __ctx) { unnamedContextParametersProperty } }
    return _result
}

@ExportedBridge("__root___unnamedContextParametersProperty_set__TypesOfArguments__Swift_Int32_main_ContextA_main_Context__")
public fun __root___unnamedContextParametersProperty_set__TypesOfArguments__Swift_Int32_main_ContextA_main_Context__(value: Int, _1: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Boolean {
    val __value = value
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = run { context(___1, __ctx) { unnamedContextParametersProperty = __value } }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_ContextB_main_ContextA_Swift_String_Swift_Int32__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_ContextB_main_ContextA_Swift_String_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, ctx0: kotlin.native.internal.NativePtr, ctx1: kotlin.native.internal.NativePtr, _3: kotlin.native.internal.NativePtr, _4: Int): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val __ctx0 = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx0) as ContextB
    val __ctx1 = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx1) as ContextA
    val ___3 = interpretObjCPointer<kotlin.String>(_3)
    val ___4 = _4
    val _result = run { (__pointerToBlock as Function4<ContextB, ContextA, kotlin.String, Int, Unit>).invoke(__ctx0, __ctx1, ___3, ___4) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Context_Swift_Int32__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Context_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, ctx0: kotlin.native.internal.NativePtr, _2: Int): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val __ctx0 = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx0) as Context
    val ___2 = _2
    val _result = run { (__pointerToBlock as Function2<Context, Int, Unit>).invoke(__ctx0, ___2) }
    return run { _result; true }
}
