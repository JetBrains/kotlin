@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Bar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Foo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Nar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3NarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.StateFoo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE8StateFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Zar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3ZarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.I1::class, "_I1")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.I1.I2::class, "__ExportedKotlinPackages_namespace_I1_I2")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("namespace_Bar_foo")
public fun namespace_Bar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Bar
    val _result = __self.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Bar_init_allocate")
public fun namespace_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Bar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Bar())
}

@ExportedBridge("namespace_Bar_voo_get")
public fun namespace_Bar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Bar
    val _result = __self.voo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_foo")
public fun namespace_Foo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = __self.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_init_allocate")
public fun namespace_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Foo())
}

@ExportedBridge("namespace_Foo_voo_get")
public fun namespace_Foo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = __self.voo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_foo")
public fun namespace_Nar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Nar
    val _result = __self.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_init_allocate")
public fun namespace_Nar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Nar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Nar())
}

@ExportedBridge("namespace_Nar_voo_get")
public fun namespace_Nar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Nar
    val _result = __self.voo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_foo")
public fun namespace_StateFoo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.StateFoo
    val _result = __self.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_init_allocate")
public fun namespace_StateFoo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.StateFoo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.StateFoo())
}

@ExportedBridge("namespace_StateFoo_voo_get")
public fun namespace_StateFoo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.StateFoo
    val _result = __self.voo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_foo")
public fun namespace_Zar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Zar
    val _result = __self.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_init_allocate")
public fun namespace_Zar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.Zar>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, namespace.Zar())
}

@ExportedBridge("namespace_Zar_voo_get")
public fun namespace_Zar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Zar
    val _result = __self.voo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
