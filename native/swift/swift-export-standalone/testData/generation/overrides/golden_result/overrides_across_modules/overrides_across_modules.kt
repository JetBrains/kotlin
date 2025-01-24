@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Cousin::class, "24overrides_across_modules6CousinC")
@file:kotlin.native.internal.objc.BindClassToObjCName(FinalDerived3::class, "24overrides_across_modules13FinalDerived3C")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Cousin_finalOverrideFunc")
public fun Cousin_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Cousin
    __self.finalOverrideFunc()
}

@ExportedBridge("Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32__")
public fun Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Cousin
    val __arg = arg
    val _result = __self.primitiveTypeFunc(__arg)
    return _result
}

@ExportedBridge("Cousin_primitiveTypeVar_get")
public fun Cousin_primitiveTypeVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Cousin
    val _result = __self.primitiveTypeVar
    return _result
}

@ExportedBridge("FinalDerived3_abstractFun1")
public fun FinalDerived3_abstractFun1(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as FinalDerived3
    __self.abstractFun1()
}

@ExportedBridge("__root___Cousin_init_allocate")
public fun __root___Cousin_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Cousin>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Cousin_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__")
public fun __root___Cousin_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = interpretObjCPointer<kotlin.String>(value)
    kotlin.native.internal.initInstance(____kt, Cousin(__value))
}

@ExportedBridge("__root___FinalDerived3_init_allocate")
public fun __root___FinalDerived3_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<FinalDerived3>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, FinalDerived3())
}

@ExportedBridge("__root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, x: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = x
    kotlin.native.internal.initInstance(____kt, FinalDerived3(__x))
}

