@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.bar.Bar::class, "4main3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.baz.Baz::class, "4main3BazC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_kotlin_baz_Baz_init_allocate")
public fun org_kotlin_baz_Baz_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.baz.Baz>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_baz_Baz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_baz_Baz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.baz.Baz()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_Foo_init_allocate")
public fun org_kotlin_foo_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.Foo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.Foo()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_bar_Bar_init_allocate")
public fun org_kotlin_foo_bar_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.bar.Bar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_bar_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_bar_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.bar.Bar()) }
    return run { _result; true }
}
