@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(DemoCrossModuleInterfaceUsage::class, "22public_interface_usage29DemoCrossModuleInterfaceUsageC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___DemoCrossModuleInterfaceUsage_init_allocate")
public fun __root___DemoCrossModuleInterfaceUsage_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DemoCrossModuleInterfaceUsage>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DemoCrossModuleInterfaceUsage_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___DemoCrossModuleInterfaceUsage_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, DemoCrossModuleInterfaceUsage())
}
