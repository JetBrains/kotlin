@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Bar::class, "_Bar")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Foo::class, "_Foo")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassC::class, "10edge_cases6ClassCC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Baz::class, "_Baz")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Bar.Conflict::class, "10edge_cases59_ExportedKotlinPackages_conflictingTypealiases_Bar_ConflictC")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Foo.Conflict::class, "10edge_cases59_ExportedKotlinPackages_conflictingTypealiases_Foo_ConflictC")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceA::class, "_InterfaceA")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceB::class, "_InterfaceB")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
internal external fun Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(self: kotlin.native.internal.NativePtr, result: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Baz::class, "foo")
public fun Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse(self: Baz, result: kotlin.Any): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin.native.internal.ref.createRetainedExternalRCRef(result)
    val _result = Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(__self, __result)
    return run<Unit> { _result }
}

@ImportedBridge("InterfaceA_foo__reverse_swift")
internal external fun InterfaceA_foo__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InterfaceA::class, "foo")
public fun InterfaceA_foo__reverse(self: InterfaceA): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InterfaceA_foo__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("InterfaceB_bar__reverse_swift")
internal external fun InterfaceB_bar__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InterfaceB::class, "bar")
public fun InterfaceB_bar__reverse(self: InterfaceB): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InterfaceB_bar__reverse_swift(__self)
    return run<Unit> { _result }
}

@ExportedBridge("Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, result: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Baz
    val __result = kotlin.native.internal.ref.dereferenceExternalRCRef(result) as kotlin.Any
    val _result = run { __self.foo(__result) }
    return run { _result; true }
}

@ExportedBridge("ClassC_baz")
public fun ClassC_baz(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassC
    val _result = run { __self.baz() }
    return run { _result; true }
}

@ExportedBridge("InterfaceA_foo")
public fun InterfaceA_foo(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InterfaceA
    val _result = run { __self.foo() }
    return run { _result; true }
}

@ExportedBridge("InterfaceB_bar")
public fun InterfaceB_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InterfaceB
    val _result = run { __self.bar() }
    return run { _result; true }
}

@ExportedBridge("conflictingTypealiases_Bar_Conflict_init_allocate")
public fun conflictingTypealiases_Bar_Conflict_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<conflictingTypealiases.Bar.Conflict>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, conflictingTypealiases.Bar.Conflict()) }
    return run { _result; true }
}

@ExportedBridge("conflictingTypealiases_Foo_Conflict_init_allocate")
public fun conflictingTypealiases_Foo_Conflict_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<conflictingTypealiases.Foo.Conflict>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, conflictingTypealiases.Foo.Conflict()) }
    return run { _result; true }
}
