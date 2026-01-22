@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ConcreteSelfReferencing::class, "14f_bounded_type23ConcreteSelfReferencingC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SelfReferencing::class, "14f_bounded_type15SelfReferencingC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyComparable::class, "_MyComparable")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("MyComparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyComparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyComparable<kotlin.Any?>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("SelfReferencing_compareTo__TypesOfArguments__f_bounded_type_SelfReferencing__")
public fun SelfReferencing_compareTo__TypesOfArguments__f_bounded_type_SelfReferencing__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SelfReferencing<SelfReferencing<*>>
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as SelfReferencing<SelfReferencing<*>>
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("__root___ConcreteSelfReferencing_init_allocate")
public fun __root___ConcreteSelfReferencing_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ConcreteSelfReferencing>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ConcreteSelfReferencing())
}
