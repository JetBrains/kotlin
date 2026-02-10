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
    val _result = context(__ctx) { __self.bar(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Foo_baz_get__TypesOfArguments__main_Context__")
public fun Foo_baz_get__TypesOfArguments__main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = context(__ctx) { __self.baz }
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
    val _result = context(__contextA, __context, __contextB) { __self.run { __receiver.complexContextFunction(__count) } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextB_main_ContextA__")
public fun Foo_complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextB_main_ContextA__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val _result = context(__contextB, __contextA) { __self.run { __receiver.complexContextProperty } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Int32_main_ContextB_main_ContextA__")
public fun Foo_complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Int32_main_ContextB_main_ContextA__(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, value: Int, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    context(__contextB, __contextA) { __self.run { __receiver.complexContextProperty = __value } }
}

@ExportedBridge("Foo_foo__TypesOfArguments__main_Context__")
public fun Foo_foo__TypesOfArguments__main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    context(__ctx) { __self.foo() }
}

@ExportedBridge("Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__")
public fun Foo_unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(self: kotlin.native.internal.NativePtr, _0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as Context
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextB
    context(___0, __ctx) { __self.unnamedContextParametersFunction() }
}

@ExportedBridge("Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__")
public fun Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as Context
    val _result = context(__ctx, ___1) { __self.unnamedContextParametersProperty }
    return _result.objcPtr()
}

@ExportedBridge("Foo_unnamedContextParametersProperty_set__TypesOfArguments__Swift_String_main_ContextA_main_Context__")
public fun Foo_unnamedContextParametersProperty_set__TypesOfArguments__Swift_String_main_ContextA_main_Context__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _2: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __value = interpretObjCPointer<kotlin.String>(value)
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___2 = kotlin.native.internal.ref.dereferenceExternalRCRef(_2) as Context
    context(__ctx, ___2) { __self.unnamedContextParametersProperty = __value }
}

@ExportedBridge("__root___ContextA_init_allocate")
public fun __root___ContextA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ContextA>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ContextA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ContextA())
}

@ExportedBridge("__root___ContextB_init_allocate")
public fun __root___ContextB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ContextB>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ContextB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ContextB())
}

@ExportedBridge("__root___Context_init_allocate")
public fun __root___Context_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Context>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Context_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Context())
}

@ExportedBridge("__root___Foo_get")
public fun __root___Foo_get(): kotlin.native.internal.NativePtr {
    val _result = Foo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__")
public fun __root___bar__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_main_Context__(arg: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = context(__ctx) { bar(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz_get__TypesOfArguments__main_Context__")
public fun __root___baz_get__TypesOfArguments__main_Context__(ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = context(__ctx) { baz }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___complexContextFunction__TypesOfArguments__Swift_String_Swift_Bool_main_Context_main_ContextA_main_ContextB__")
public fun __root___complexContextFunction__TypesOfArguments__Swift_String_Swift_Bool_main_Context_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, yes: Boolean, context: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Int {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __yes = yes
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as Context
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = context(__context, __contextA, __contextB) { __receiver.complexContextFunction(__yes) }
    return _result
}

@ExportedBridge("__root___complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextA_main_ContextB__")
public fun __root___complexContextProperty_get__TypesOfArguments__Swift_String_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val _result = context(__contextA, __contextB) { __receiver.complexContextProperty }
    return _result
}

@ExportedBridge("__root___complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Bool_main_ContextA_main_ContextB__")
public fun __root___complexContextProperty_set__TypesOfArguments__Swift_String_Swift_Bool_main_ContextA_main_ContextB__(`receiver`: kotlin.native.internal.NativePtr, value: Boolean, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr): Unit {
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    context(__contextA, __contextB) { __receiver.complexContextProperty = __value }
}

@ExportedBridge("__root___foo__TypesOfArguments__main_Context__")
public fun __root___foo__TypesOfArguments__main_Context__(ctx: kotlin.native.internal.NativePtr): Unit {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    context(__ctx) { foo() }
}

@ExportedBridge("__root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__")
public fun __root___unnamedContextParametersFunction__TypesOfArguments__main_Context_main_ContextB__(ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Unit {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as ContextB
    context(__ctx, ___1) { unnamedContextParametersFunction() }
}

@ExportedBridge("__root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__")
public fun __root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(_0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Int {
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = context(___0, __ctx) { unnamedContextParametersProperty }
    return _result
}

@ExportedBridge("__root___unnamedContextParametersProperty_set__TypesOfArguments__Swift_Int32_main_ContextA_main_Context__")
public fun __root___unnamedContextParametersProperty_set__TypesOfArguments__Swift_Int32_main_ContextA_main_Context__(value: Int, _1: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Unit {
    val __value = value
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    context(___1, __ctx) { unnamedContextParametersProperty = __value }
}
