@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Foo_value_get")
public fun Foo_value_get(self: kotlin.native.internal.NativePtr): Any {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Foo<*>
    val _result = __self.value
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result) as Any
}

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo<Any>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Any__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Any__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as Any
    kotlin.native.internal.initInstance(____kt, Foo<Any>(__value))
}
