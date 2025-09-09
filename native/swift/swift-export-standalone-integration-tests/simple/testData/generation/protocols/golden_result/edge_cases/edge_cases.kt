@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Bar::class, "_Bar")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Foo::class, "_Foo")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Bar.Conflict::class, "10edge_cases59_ExportedKotlinPackages_conflictingTypealiases_Bar_ConflictC")
@file:kotlin.native.internal.objc.BindClassToObjCName(conflictingTypealiases.Foo.Conflict::class, "10edge_cases59_ExportedKotlinPackages_conflictingTypealiases_Foo_ConflictC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("conflictingTypealiases_Bar_Conflict_init_allocate")
public fun conflictingTypealiases_Bar_Conflict_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<conflictingTypealiases.Bar.Conflict>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, conflictingTypealiases.Bar.Conflict())
}

@ExportedBridge("conflictingTypealiases_Foo_Conflict_init_allocate")
public fun conflictingTypealiases_Foo_Conflict_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<conflictingTypealiases.Foo.Conflict>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, conflictingTypealiases.Foo.Conflict())
}
