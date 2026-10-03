@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(koin.like.Module::class, "22ExportedKotlinPackages4koinO4likeO4mainE6ModuleC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("koin_like_Module_init_allocate")
public fun koin_like_Module_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<koin.like.Module>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("koin_like_Module_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun koin_like_Module_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, koin.like.Module()) }
    return run { _result; true }
}
