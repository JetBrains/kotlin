@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Foo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Foo()) }
    return run { _result; true }
}

@ExportedBridge("__root___foo_get")
public fun __root___foo_get(): kotlin.native.internal.NativePtr {
    val _result = run { foo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_set__TypesOfArguments__main_Foo__")
public fun __root___foo_set__TypesOfArguments__main_Foo__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foo
    val _result = run { foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___getFoo")
public fun __root___getFoo(): kotlin.native.internal.NativePtr {
    val _result = run { getFoo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
