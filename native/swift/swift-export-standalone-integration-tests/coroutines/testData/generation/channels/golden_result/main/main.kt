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

@ExportedBridge("__root___produceChannel")
public fun __root___produceChannel(): kotlin.native.internal.NativePtr {
    val _result = run { produceChannel() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produceIntChannel")
public fun __root___produceIntChannel(): kotlin.native.internal.NativePtr {
    val _result = run { produceIntChannel() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produceNullableChannel")
public fun __root___produceNullableChannel(): kotlin.native.internal.NativePtr {
    val _result = run { produceNullableChannel() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produceStringChannel")
public fun __root___produceStringChannel(): kotlin.native.internal.NativePtr {
    val _result = run { produceStringChannel() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
