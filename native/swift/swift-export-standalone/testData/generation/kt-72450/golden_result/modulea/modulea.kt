@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(namespace.modulea.ClassFromA::class, "22ExportedKotlinPackages9namespaceO7moduleaO7moduleaE10ClassFromAC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("namespace_modulea_ClassFromA_init_allocate")
public fun namespace_modulea_ClassFromA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<namespace.modulea.ClassFromA>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("namespace_modulea_ClassFromA_init_initialize__TypesOfArguments__Swift_UInt__")
public fun namespace_modulea_ClassFromA_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, namespace.modulea.ClassFromA())
}

