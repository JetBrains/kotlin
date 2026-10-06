@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(BaseDriver::class, "4main10BaseDriverC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Driver::class, "_main_Driver")
@file:kotlin.native.internal.objc.BindClassToObjCName(ExtensionVararg::class, "_main_ExtensionVararg")
@file:kotlin.native.internal.objc.BindClassToObjCName(KeywordLabels::class, "_main_KeywordLabels")
@file:kotlin.native.internal.objc.BindClassToObjCName(Driver.Listener::class, "_main__Driver_Listener")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

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

@ImportedBridge("ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse_swift")
internal external fun ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse_swift(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, parts: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(ExtensionVararg::class, "join")
public fun ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse(self: ExtensionVararg, `receiver`: kotlin.String, parts: kotlin.Array<out kotlin.String>): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __receiver = `receiver`.objcPtr()
    val __parts = parts.toList().objcPtr()
    val _result = ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse_swift(__self, __receiver, __parts)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift")
internal external fun KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(self: kotlin.native.internal.NativePtr, inout: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(KeywordLabels::class, "count")
public fun KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse(self: KeywordLabels, inout: kotlin.IntArray): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __inout = inout.toList().objcPtr()
    val _result = KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(__self, __inout)
    return _result
}

@ImportedBridge("KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, inout: Int): Int

@BindReverseBridgeToMethod(KeywordLabels::class, "double")
public fun KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse(self: KeywordLabels, inout: Int): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse_swift(__self, inout)
    return _result
}

@ExportedBridge("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___")
public fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int>>(queryKeys).toIntArray()
    __self.addInts(*__queryKeys)
    return true
}

@ExportedBridge("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg____direct", nonVirtualTargetMethod = "addInts")
public fun BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int>>(queryKeys).toIntArray()
    __self.addInts(*__queryKeys)
    return true
}

@ExportedBridge("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg___")
public fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    __self.addListener(*__queryKeys)
    return true
}

@ExportedBridge("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg____direct", nonVirtualTargetMethod = "addListener")
public fun BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    __self.addListener(*__queryKeys)
    return true
}

@ExportedBridge("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg___")
public fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg___(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int?>>(queryKeys).toTypedArray()
    __self.addOptionalInts(*__queryKeys)
    return true
}

@ExportedBridge("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg____direct", nonVirtualTargetMethod = "addOptionalInts")
public fun BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg____direct(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseDriver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<Int?>>(queryKeys).toTypedArray()
    __self.addOptionalInts(*__queryKeys)
    return true
}

@ExportedBridge("Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__")
public fun Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__(self: kotlin.native.internal.NativePtr, queryKeys: kotlin.native.internal.NativePtr, listener: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Driver
    val __queryKeys = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(queryKeys).toTypedArray()
    val __listener = kotlin.native.internal.ref.dereferenceExternalRCRef(listener) as Driver.Listener
    __self.addListener(*__queryKeys, listener = __listener)
    return true
}

@ExportedBridge("ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg___")
public fun ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg___(self: kotlin.native.internal.NativePtr, `receiver`: kotlin.native.internal.NativePtr, parts: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExtensionVararg
    val __receiver = interpretObjCPointer<kotlin.String>(`receiver`)
    val __parts = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(parts).toTypedArray()
    val _result = __self.run { __receiver.join(*__parts) }
    return _result.objcPtr()
}

@ExportedBridge("KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___")
public fun KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___(self: kotlin.native.internal.NativePtr, inout: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KeywordLabels
    val __inout = interpretObjCPointer<kotlin.collections.List<Int>>(inout).toIntArray()
    val _result = __self.count(*__inout)
    return _result
}

@ExportedBridge("KeywordLabels_double__TypesOfArguments__Swift_Int32__")
public fun KeywordLabels_double__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, inout: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KeywordLabels
    val __inout = inout
    val _result = __self.double(__inout)
    return _result
}

@ExportedBridge("__root___BaseDriver_init_allocate")
public fun __root___BaseDriver_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<BaseDriver>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, BaseDriver())
    return true
}
