@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(INHERITANCE_SINGLE_CLASS::class, "11inheritance24INHERITANCE_SINGLE_CLASSC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OBJECT_WITH_CLASS_INHERITANCE::class, "11inheritance29OBJECT_WITH_CLASS_INHERITANCEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OPEN_CLASS::class, "11inheritance10OPEN_CLASSC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("INHERITANCE_SINGLE_CLASS_value_get")
public fun INHERITANCE_SINGLE_CLASS_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INHERITANCE_SINGLE_CLASS
    val _result = __self.value
    return _result
}

@ExportedBridge("INHERITANCE_SINGLE_CLASS_value_set__TypesOfArguments__Swift_Int32__")
public fun INHERITANCE_SINGLE_CLASS_value_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as INHERITANCE_SINGLE_CLASS
    val __newValue = newValue
    __self.value = __newValue
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_allocate")
public fun __root___INHERITANCE_SINGLE_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<INHERITANCE_SINGLE_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    kotlin.native.internal.initInstance(____kt, INHERITANCE_SINGLE_CLASS(__value))
}

@ExportedBridge("__root___OBJECT_WITH_CLASS_INHERITANCE_get")
public fun __root___OBJECT_WITH_CLASS_INHERITANCE_get(): kotlin.native.internal.NativePtr {
    val _result = OBJECT_WITH_CLASS_INHERITANCE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_allocate")
public fun __root___OPEN_CLASS_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OPEN_CLASS>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OPEN_CLASS())
}

