@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Accessor::class, "4main8AccessorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Accessor.Inner::class, "4main8AccessorC5InnerC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Accessor_Inner_init_allocate")
public fun Accessor_Inner_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Accessor.Inner>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Accessor_Inner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double_Swift_Array_Swift_Bool__main_Accessor__")
public fun Accessor_Inner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double_Swift_Array_Swift_Bool__main_Accessor__(__kt: kotlin.native.internal.NativePtr, y: Double, z: kotlin.native.internal.NativePtr, outer__: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __y = y
    val __z = interpretObjCPointer<kotlin.collections.List<Boolean>>(z).toBooleanArray()
    val __outer__ = kotlin.native.internal.ref.dereferenceExternalRCRef(outer__) as Accessor
    kotlin.native.internal.initInstance(____kt, (__outer__ as Accessor).Inner(__y, *__z))
}

@ExportedBridge("Accessor_Inner_y_get")
public fun Accessor_Inner_y_get(self: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor.Inner
    val _result = __self.y
    return _result
}

@ExportedBridge("Accessor_Inner_z_get")
public fun Accessor_Inner_z_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor.Inner
    val _result = __self.z
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Accessor_Inner_z_set__TypesOfArguments__ExportedKotlinPackages_kotlin_BooleanArray__")
public fun Accessor_Inner_z_set__TypesOfArguments__ExportedKotlinPackages_kotlin_BooleanArray__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor.Inner
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.BooleanArray
    __self.z = __newValue
}

@ExportedBridge("Accessor_get__TypesOfArguments__Swift_Int32__")
public fun Accessor_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, i: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor
    val __i = i
    val _result = __self.`get`(__i)
    return _result
}

@ExportedBridge("Accessor_x_get")
public fun Accessor_x_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor
    val _result = __self.x
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Accessor_init_allocate")
public fun __root___Accessor_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Accessor>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Accessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___")
public fun __root___Accessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(__kt: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = interpretObjCPointer<kotlin.collections.List<Int>>(x).toIntArray()
    kotlin.native.internal.initInstance(____kt, Accessor(*__x))
}

@ExportedBridge("__root___asNumberList__TypesOfArguments__Swift_Array_ExportedKotlinPackages_kotlin_Number___")
public fun __root___asNumberList__TypesOfArguments__Swift_Array_ExportedKotlinPackages_kotlin_Number___(x: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __x = interpretObjCPointer<kotlin.collections.List<kotlin.Number>>(x).toTypedArray()
    val _result = asNumberList(*__x)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("__root___extension__TypesOfArguments__main_Accessor_Swift_Array_Swift_Double___")
public fun __root___extension__TypesOfArguments__main_Accessor_Swift_Array_Swift_Double___(`receiver`: kotlin.native.internal.NativePtr, d: kotlin.native.internal.NativePtr): Unit {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Accessor
    val __d = interpretObjCPointer<kotlin.collections.List<Double>>(d).toDoubleArray()
    __receiver.extension(*__d)
}

@ExportedBridge("__root___oneMore__TypesOfArguments__Swift_Array_Swift_String__Swift_Int32__")
public fun __root___oneMore__TypesOfArguments__Swift_Array_Swift_String__Swift_Int32__(a: kotlin.native.internal.NativePtr, b: Int): Unit {
    val __a = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(a).toTypedArray()
    val __b = b
    oneMore(*__a, b = __b)
}

@ExportedBridge("__root___simple__TypesOfArguments__Swift_Array_Swift_String___")
public fun __root___simple__TypesOfArguments__Swift_Array_Swift_String___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(s).toTypedArray()
    val _result = simple(*__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___withDefault__TypesOfArguments__Swift_Array_Swift_String__Swift_Int32__")
public fun __root___withDefault__TypesOfArguments__Swift_Array_Swift_String__Swift_Int32__(a: kotlin.native.internal.NativePtr, b: Int): Unit {
    val __a = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(a).toTypedArray()
    val __b = b
    withDefault(*__a, b = __b)
}
