@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Bar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3BarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Foo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3FooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.MutableSharedFoo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE16MutableSharedFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.MutableStateFoo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE15MutableStateFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Nar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3NarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.SharedFoo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE9SharedFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.StateFoo::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE8StateFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.Zar::class, "22ExportedKotlinPackages9namespaceO14flow_overridesE3ZarC")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.I1::class, "_I1")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.I1.I2::class, "__ExportedKotlinPackages_namespace_I1_I2")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("namespace_Bar_foo__reverse_swift")
internal external fun namespace_Bar_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.Bar::class, "foo")
public fun namespace_Bar_foo__reverse(self: namespace.Bar): kotlinx.coroutines.flow.Flow<namespace.I1.I2> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_Bar_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.Flow<namespace.I1.I2>
}

@ImportedBridge("namespace_Foo_foo__reverse_swift")
internal external fun namespace_Foo_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.Foo::class, "foo")
public fun namespace_Foo_foo__reverse(self: namespace.Foo): kotlinx.coroutines.flow.Flow<namespace.I1?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_Foo_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.Flow<namespace.I1?>
}

@ImportedBridge("namespace_MutableSharedFoo_foo__reverse_swift")
internal external fun namespace_MutableSharedFoo_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.MutableSharedFoo::class, "foo")
public fun namespace_MutableSharedFoo_foo__reverse(self: namespace.MutableSharedFoo): kotlinx.coroutines.flow.MutableSharedFlow<namespace.I1?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_MutableSharedFoo_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.MutableSharedFlow<namespace.I1?>
}

@ImportedBridge("namespace_MutableStateFoo_foo__reverse_swift")
internal external fun namespace_MutableStateFoo_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.MutableStateFoo::class, "foo")
public fun namespace_MutableStateFoo_foo__reverse(self: namespace.MutableStateFoo): kotlinx.coroutines.flow.MutableStateFlow<namespace.I1?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_MutableStateFoo_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.MutableStateFlow<namespace.I1?>
}

@ImportedBridge("namespace_Nar_foo__reverse_swift")
internal external fun namespace_Nar_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.Nar::class, "foo")
public fun namespace_Nar_foo__reverse(self: namespace.Nar): kotlinx.coroutines.flow.Flow<Nothing> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_Nar_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.Flow<Nothing>
}

@ImportedBridge("namespace_SharedFoo_foo__reverse_swift")
internal external fun namespace_SharedFoo_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.SharedFoo::class, "foo")
public fun namespace_SharedFoo_foo__reverse(self: namespace.SharedFoo): kotlinx.coroutines.flow.SharedFlow<namespace.I1?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_SharedFoo_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.SharedFlow<namespace.I1?>
}

@ImportedBridge("namespace_StateFoo_foo__reverse_swift")
internal external fun namespace_StateFoo_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.StateFoo::class, "foo")
public fun namespace_StateFoo_foo__reverse(self: namespace.StateFoo): kotlinx.coroutines.flow.StateFlow<namespace.I1?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_StateFoo_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.StateFlow<namespace.I1?>
}

@ImportedBridge("namespace_Zar_foo__reverse_swift")
internal external fun namespace_Zar_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(namespace.Zar::class, "foo")
public fun namespace_Zar_foo__reverse(self: namespace.Zar): kotlinx.coroutines.flow.Flow<namespace.I1.I2?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = namespace_Zar_foo__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlinx.coroutines.flow.Flow<namespace.I1.I2?>
}

@ExportedBridge("namespace_Bar_foo")
public fun namespace_Bar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Bar
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Bar_init_allocate")
public fun namespace_Bar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.Bar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.Bar()) }
    return run { _result; true }
}

@ExportedBridge("namespace_Bar_voo_get")
public fun namespace_Bar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Bar
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_foo")
public fun namespace_Foo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_init_allocate")
public fun namespace_Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.Foo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.Foo()) }
    return run { _result; true }
}

@ExportedBridge("namespace_Foo_voo_get")
public fun namespace_Foo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Foo
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableSharedFoo_foo")
public fun namespace_MutableSharedFoo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.MutableSharedFoo
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableSharedFoo_init_allocate")
public fun namespace_MutableSharedFoo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.MutableSharedFoo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableSharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_MutableSharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.MutableSharedFoo()) }
    return run { _result; true }
}

@ExportedBridge("namespace_MutableSharedFoo_voo_get")
public fun namespace_MutableSharedFoo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.MutableSharedFoo
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableStateFoo_foo")
public fun namespace_MutableStateFoo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.MutableStateFoo
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableStateFoo_init_allocate")
public fun namespace_MutableStateFoo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.MutableStateFoo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_MutableStateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_MutableStateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.MutableStateFoo()) }
    return run { _result; true }
}

@ExportedBridge("namespace_MutableStateFoo_voo_get")
public fun namespace_MutableStateFoo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.MutableStateFoo
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_foo")
public fun namespace_Nar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Nar
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_init_allocate")
public fun namespace_Nar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.Nar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Nar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.Nar()) }
    return run { _result; true }
}

@ExportedBridge("namespace_Nar_voo_get")
public fun namespace_Nar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Nar
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_SharedFoo_foo")
public fun namespace_SharedFoo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.SharedFoo
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_SharedFoo_init_allocate")
public fun namespace_SharedFoo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.SharedFoo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_SharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_SharedFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.SharedFoo()) }
    return run { _result; true }
}

@ExportedBridge("namespace_SharedFoo_voo_get")
public fun namespace_SharedFoo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.SharedFoo
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_foo")
public fun namespace_StateFoo_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.StateFoo
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_init_allocate")
public fun namespace_StateFoo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.StateFoo>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_StateFoo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.StateFoo()) }
    return run { _result; true }
}

@ExportedBridge("namespace_StateFoo_voo_get")
public fun namespace_StateFoo_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.StateFoo
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_foo")
public fun namespace_Zar_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Zar
    val _result = run { __self.foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_init_allocate")
public fun namespace_Zar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<namespace.Zar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun namespace_Zar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, namespace.Zar()) }
    return run { _result; true }
}

@ExportedBridge("namespace_Zar_voo_get")
public fun namespace_Zar_voo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as namespace.Zar
    val _result = run { __self.voo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
