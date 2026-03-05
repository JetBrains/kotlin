@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(CLASS_ACROSS_MODULES::class, "24cross_module_inheritance20CLASS_ACROSS_MODULESC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("CLASS_ACROSS_MODULES_value_get")
public fun CLASS_ACROSS_MODULES_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as CLASS_ACROSS_MODULES
    val _result = run { __self.value }
    return _result
}

@ExportedBridge("CLASS_ACROSS_MODULES_value_set__TypesOfArguments__Swift_Int32__")
public fun CLASS_ACROSS_MODULES_value_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as CLASS_ACROSS_MODULES
    val __newValue = newValue
    val _result = run { __self.value = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___CLASS_ACROSS_MODULES_init_allocate")
public fun __root___CLASS_ACROSS_MODULES_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<CLASS_ACROSS_MODULES>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CLASS_ACROSS_MODULES_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___CLASS_ACROSS_MODULES_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    val _result = run { kotlin.native.internal.initInstance(____kt, CLASS_ACROSS_MODULES(__value)) }
    return run { _result; true }
}
