@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.operatorFunctions.operatorFunctions.Vector::class, "22ExportedKotlinPackages10generationO17operatorFunctionsO17operatorFunctionsO4mainE6VectorC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_compareTo__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__")
public fun generation_operatorFunctions_operatorFunctions_Vector_compareTo__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_div__TypesOfArguments__Swift_Int32__")
public fun generation_operatorFunctions_operatorFunctions_Vector_div__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, scalar: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __scalar = scalar
    val _result = __self.div(__scalar)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_get__TypesOfArguments__Swift_Int32__")
public fun generation_operatorFunctions_operatorFunctions_Vector_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_init_allocate")
public fun generation_operatorFunctions_operatorFunctions_Vector_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.operatorFunctions.operatorFunctions.Vector>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
public fun generation_operatorFunctions_operatorFunctions_Vector_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, x: Int, y: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = x
    val __y = y
    kotlin.native.internal.initInstance(____kt, generation.operatorFunctions.operatorFunctions.Vector(__x, __y))
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_minus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__")
public fun generation_operatorFunctions_operatorFunctions_Vector_minus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.minus(__other)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_plus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__")
public fun generation_operatorFunctions_operatorFunctions_Vector_plus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.plus(__other)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_times__TypesOfArguments__Swift_Int32__")
public fun generation_operatorFunctions_operatorFunctions_Vector_times__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, scalar: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val __scalar = scalar
    val _result = __self.times(__scalar)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_toString")
public fun generation_operatorFunctions_operatorFunctions_Vector_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_unaryMinus")
public fun generation_operatorFunctions_operatorFunctions_Vector_unaryMinus(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.unaryMinus()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_x_get")
public fun generation_operatorFunctions_operatorFunctions_Vector_x_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.x
    return _result
}

@ExportedBridge("generation_operatorFunctions_operatorFunctions_Vector_y_get")
public fun generation_operatorFunctions_operatorFunctions_Vector_y_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.operatorFunctions.operatorFunctions.Vector
    val _result = __self.y
    return _result
}
