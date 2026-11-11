@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ConcreteSelfReferencing::class, "14f_bounded_type23ConcreteSelfReferencingC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyComparable::class, "_f_bounded_type_MyComparable")
@file:kotlin.native.internal.objc.BindClassToObjCName(SelfReferencing::class, "14f_bounded_type21_SelfReferencing_BaseC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("MyComparable_compareTo__TypesOfArguments__T____reverse_swift")
internal external fun MyComparable_compareTo__TypesOfArguments__T____reverse_swift(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(MyComparable::class, "compareTo")
public fun MyComparable_compareTo__TypesOfArguments__T____reverse(self: MyComparable<kotlin.Any?>, other: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __other = if (other == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(other)
    val _result = MyComparable_compareTo__TypesOfArguments__T____reverse_swift(__self, __other)
    return _result
}

@ImportedBridge("SelfReferencing_compareTo__TypesOfArguments__T____reverse_swift")
internal external fun SelfReferencing_compareTo__TypesOfArguments__T____reverse_swift(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(SelfReferencing::class, "compareTo")
public fun SelfReferencing_compareTo__TypesOfArguments__T____reverse(self: SelfReferencing<*>, other: SelfReferencing<*>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __other = if (other == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(other)
    val _result = SelfReferencing_compareTo__TypesOfArguments__T____reverse_swift(__self, __other)
    return _result
}

@ExportedBridge("BaseClass_v_get")
public fun BaseClass_v_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseClass<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : MyComparable<__T0>> __bridge(): MyComparable<*> = ((__self as BaseClass<__T0>).v as kotlin.Any?) as MyComparable<*>
        __bridge<Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("BaseClass_v_set__TypesOfArguments__T_f_bounded_type_MyComparable__")
public fun BaseClass_v_set__TypesOfArguments__T_f_bounded_type_MyComparable__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as BaseClass<*>
    val __newValue = (dereferenceAndDisposeBridgeableRef(newValue) as MyComparable<*>)
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : MyComparable<__T0>> __bridge() { (__self as BaseClass<__T0>).v = (__newValue as __T0) }
        __bridge<Nothing>()
    }
                    return true
}

@ExportedBridge("MyComparable_compareTo__TypesOfArguments__T__")
public fun MyComparable_compareTo__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyComparable<kotlin.Any?>
    val __other = (dereferenceAndDisposeBridgeableRef(other) as kotlin.Any?)
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("SelfReferencing_compareTo__TypesOfArguments__T__")
public fun SelfReferencing_compareTo__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SelfReferencing<*>
    val __other = (dereferenceAndDisposeBridgeableRef(other) as SelfReferencing<*>)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : SelfReferencing<__T0>> __bridge(): Int = ((__self as SelfReferencing<__T0>).compareTo((__other as __T0)) as kotlin.Any?) as Int
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("SelfReferencing_compareTo__TypesOfArguments__T___direct", nonVirtualTargetMethod = "compareTo")
public fun SelfReferencing_compareTo__TypesOfArguments__T___direct(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SelfReferencing<*>
    val __other = (dereferenceAndDisposeBridgeableRef(other) as SelfReferencing<*>)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : SelfReferencing<__T0>> __bridge(): Int = ((__self as SelfReferencing<__T0>).compareTo((__other as __T0)) as kotlin.Any?) as Int
        __bridge<Nothing>()
    }
                    return _result
}

@ExportedBridge("__root___BaseClass_init_allocate")
public fun __root___BaseClass_init_allocate(): kotlin.native.internal.NativePtr {
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : MyComparable<__T0>> __alloc(): kotlin.Any? = kotlin.native.internal.createUninitializedInstance<BaseClass<__T0>>()
        __alloc<Nothing>()
    }
                    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___BaseClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T_f_bounded_type_MyComparable__")
public fun __root___BaseClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T_f_bounded_type_MyComparable__(__kt: kotlin.native.internal.NativePtr, v: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __v = (dereferenceAndDisposeBridgeableRef(v) as MyComparable<*>)
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : MyComparable<__T0>> __init() { kotlin.native.internal.initInstance(____kt, BaseClass<__T0>((__v as __T0))) }
        __init<Nothing>()
    }
                    return true
}

@ExportedBridge("__root___ConcreteSelfReferencing_init_allocate")
public fun __root___ConcreteSelfReferencing_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ConcreteSelfReferencing>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ConcreteSelfReferencing())
    return true
}

@ExportedBridge("__root___SelfReferencing_init_allocate")
public fun __root___SelfReferencing_init_allocate(): kotlin.native.internal.NativePtr {
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : SelfReferencing<__T0>> __alloc(): kotlin.Any? = kotlin.native.internal.createUninitializedInstance<SelfReferencing<__T0>>()
        __alloc<Nothing>()
    }
                    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___SelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___SelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : SelfReferencing<__T0>> __init() { kotlin.native.internal.initInstance(____kt, SelfReferencing<__T0>()) }
        __init<Nothing>()
    }
                    return true
}

@ExportedBridge("__root___testFun__TypesOfArguments__T_f_bounded_type_MyComparable__")
public fun __root___testFun__TypesOfArguments__T_f_bounded_type_MyComparable__(a: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __a = (dereferenceAndDisposeBridgeableRef(a) as MyComparable<*>)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : MyComparable<__T0>> __bridge(): MyComparable<*> = (testFun<__T0>((__a as __T0)) as kotlin.Any?) as MyComparable<*>
        __bridge<Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
