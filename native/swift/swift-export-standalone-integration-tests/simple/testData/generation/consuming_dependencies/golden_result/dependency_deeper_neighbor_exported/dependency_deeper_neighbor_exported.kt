@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(dependency.four.AnotherBar::class, "22ExportedKotlinPackages10dependencyO4fourO35dependency_deeper_neighbor_exportedE10AnotherBarC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("dependency_four_AnotherBar_init_allocate")
public fun dependency_four_AnotherBar_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<dependency.four.AnotherBar>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("dependency_four_AnotherBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun dependency_four_AnotherBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, dependency.four.AnotherBar()) }
    return run { _result; true }
}
