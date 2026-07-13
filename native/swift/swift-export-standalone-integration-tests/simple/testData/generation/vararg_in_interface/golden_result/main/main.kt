@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(BaseDriver::class, "4main10BaseDriverC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Driver::class, "_Driver")
@file:kotlin.native.internal.objc.BindClassToObjCName(Driver.Listener::class, "__Driver_Listener")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift")
internal external fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(BaseDriver::class, "addInts")
public fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse(self: BaseDriver, queryKeys: kotlin.IntArray): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __queryKeys = queryKeys.toList().objcPtr()
    val _result = BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(__self, __queryKeys)
    return run<Unit> { _result }
}

@ImportedBridge("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift")
internal external fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(BaseDriver::class, "addListener")
public fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse(self: BaseDriver, queryKeys: kotlin.Array<out kotlin.String>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __queryKeys = queryKeys.toList().objcPtr()
    val _result = BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift(__self, __queryKeys)
    return run<Unit> { _result }
}

@ImportedBridge("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift")
internal external fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(BaseDriver::class, "addOptionalInts")
public fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse(self: BaseDriver, queryKeys: kotlin.Array<out Int?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __queryKeys = queryKeys.toList().objcPtr()
    val _result = BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift(__self, __queryKeys)
    return run<Unit> { _result }
}

@ImportedBridge("Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift")
internal external fun Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr, listener: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Driver::class, "addListener")
public fun Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse(self: Driver, queryKeys: kotlin.Array<out kotlin.String>, listener: Driver.Listener): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __queryKeys = queryKeys.toList().objcPtr()
    val __listener = kotlin.native.internal.ref.createRetainedExternalRCRef(listener)
    val _result = Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift(__self, __queryKeys, __listener)
    return run<Unit> { _result }
}

@ExportedBridge("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___")
public fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int>>(queryKeys).toIntArray()
    val _result = run { __self.addInts(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg____direct", nonVirtualTargetMethod = "addInts")
public fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int>>(queryKeys).toIntArray()
    val _result = run { __self.addInts(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg___")
public fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    val _result = run { __self.addListener(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg____direct", nonVirtualTargetMethod = "addListener")
public fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    val _result = run { __self.addListener(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg___")
public fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int?>>(queryKeys).toTypedArray()
    val _result = run { __self.addOptionalInts(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg____direct", nonVirtualTargetMethod = "addOptionalInts")
public fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int?>>(queryKeys).toTypedArray()
    val _result = run { __self.addOptionalInts(*__queryKeys) }
    return run { _result; true }
}

@ExportedBridge("Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__")
public fun Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr, listener: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Driver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    val __listener = kotlin.native.internal.ref.dereferenceExternalRCRef(listener) as Driver.Listener
    val _result = run { __self.addListener(*__queryKeys, listener = __listener) }
    return run { _result; true }
}

@ExportedBridge("__root___BaseDriver_init_allocate")
public fun __root___BaseDriver_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<BaseDriver>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, BaseDriver()) }
    return run { _result; true }
}
