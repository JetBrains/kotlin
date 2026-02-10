@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Context::class, "4main7ContextC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ContextA::class, "4main8ContextAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ContextB::class, "4main8ContextBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Foo_bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun Foo_bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
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

@ExportedBridge("Foo_complexContextFunction__TypesOfArguments__main_ContextA_main_Context_main_ContextB_Swift_String_Swift_Int32__")
public fun Foo_complexContextFunction__TypesOfArguments__main_ContextA_main_Context_main_ContextB_Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, context: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, count: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as Context
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __count = count
    val _result = context(__contextA, __context, __contextB) { __self.run { __receiver.complexContextFunction(__count) } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_get__TypesOfArguments__main_ContextB_main_ContextA_Swift_String__")
public fun Foo_complexContextProperty_get__TypesOfArguments__main_ContextB_main_ContextA_Swift_String__(self: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val _result = context(__contextB, __contextA) { __self.run { __receiver.complexContextProperty } }
    return _result
}

@ExportedBridge("Foo_complexContextProperty_set__TypesOfArguments__main_ContextB_main_ContextA_Swift_String_Swift_Int32__")
public fun Foo_complexContextProperty_set__TypesOfArguments__main_ContextB_main_ContextA_Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, value: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
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
public fun Foo_unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as Context
    context(__ctx, ___1) { __self.unnamedContextParametersProperty }
}

@ExportedBridge("Foo_unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__")
public fun Foo_unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__(self: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as ContextA
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as Context
    val __value = Unit
    context(__ctx, ___1) { __self.unnamedContextParametersProperty = __value }
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

@ExportedBridge("__root___bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___bar__TypesOfArguments__main_Context_anyU20KotlinRuntimeSupport__KotlinBridgeable__(ctx: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = context(__ctx) { bar(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz_get__TypesOfArguments__main_Context__")
public fun __root___baz_get__TypesOfArguments__main_Context__(ctx: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val _result = context(__ctx) { baz }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___complexContextFunction__TypesOfArguments__main_Context_main_ContextA_main_ContextB_Swift_String_Swift_Bool__")
public fun __root___complexContextFunction__TypesOfArguments__main_Context_main_ContextA_main_ContextB_Swift_String_Swift_Bool__(context: kotlin.native.internal.NativePtr, contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, yes: Boolean): Int {
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as Context
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __yes = yes
    val _result = context(__context, __contextA, __contextB) { __receiver.complexContextFunction(__yes) }
    return _result
}

@ExportedBridge("__root___complexContextProperty_get__TypesOfArguments__main_ContextA_main_ContextB_Swift_String__")
public fun __root___complexContextProperty_get__TypesOfArguments__main_ContextA_main_ContextB_Swift_String__(contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr): Boolean {
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val _result = context(__contextA, __contextB) { __receiver.complexContextProperty }
    return _result
}

@ExportedBridge("__root___complexContextProperty_set__TypesOfArguments__main_ContextA_main_ContextB_Swift_String_Swift_Bool__")
public fun __root___complexContextProperty_set__TypesOfArguments__main_ContextA_main_ContextB_Swift_String_Swift_Bool__(contextA: kotlin.native.internal.NativePtr, contextB: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, value: Boolean): Unit {
    val __contextA = kotlin.native.internal.ref.dereferenceExternalRCRef(contextA) as ContextA
    val __contextB = kotlin.native.internal.ref.dereferenceExternalRCRef(contextB) as ContextB
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __value = value
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
public fun __root___unnamedContextParametersProperty_get__TypesOfArguments__main_ContextA_main_Context__(_0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Unit {
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    context(___0, __ctx) { unnamedContextParametersProperty }
}

@ExportedBridge("__root___unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__")
public fun __root___unnamedContextParametersProperty_set__TypesOfArguments__main_ContextA_main_Context_Swift_Void__(_0: kotlin.native.internal.NativePtr, ctx: kotlin.native.internal.NativePtr): Unit {
    val ___0 = kotlin.native.internal.ref.dereferenceExternalRCRef(_0) as ContextA
    val __ctx = kotlin.native.internal.ref.dereferenceExternalRCRef(ctx) as Context
    val __value = Unit
    context(___0, __ctx) { unnamedContextParametersProperty = __value }
}
