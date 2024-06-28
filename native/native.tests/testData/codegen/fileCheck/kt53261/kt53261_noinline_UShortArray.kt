// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlin.UShortArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UShortArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UShortArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"

// CHECK-OPT: call %struct.ObjHeader* @"kfun:kotlin#<UShortArray-unbox>(kotlin.Any?){}kotlin.UShortArray?"

// CHECK-LABEL: epilogue:

fun box(): String {
    val arr1 = UShortArray(10) { it.toUShort() }
    val arr2 = UShortArray(10) { (it / 2).toUShort() }
    return if (arr1 == arr2) "FAIL" else "OK"
}
