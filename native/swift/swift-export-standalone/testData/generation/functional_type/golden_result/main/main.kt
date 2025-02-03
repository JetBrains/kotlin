@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo())
}

@ExportedBridge("__root___block_consuming_reftype_in__TypesOfArguments__U28main_FooU29202D_U20main_Foo__")
public fun __root___block_consuming_reftype_in__TypesOfArguments__U28main_FooU29202D_U20main_Foo__(b: kotlin.native.internal.NativePtr): Unit {
    val __b = { arg0: Foo ->
        val receivedBlock = interpretObjCPointer<Function1<Long, Long>>(b)
        val refToResult = receivedBlock(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0).toLong())
        refToResult.toCPointer<CPointed>()!!.asStableRef<Foo>().get()
    }
    block_consuming_reftype_in(__b)
}

