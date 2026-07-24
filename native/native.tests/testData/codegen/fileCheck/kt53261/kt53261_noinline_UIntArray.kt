// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"

// CHECK-OPT: call ptr @"kfun:kotlin#<UIntArray-unbox>(kotlin.Any?){}kotlin.UIntArray?"

// CHECK-LABEL: epilogue:

fun box(): String {
    val arr1 = UIntArray(10) { it.toUInt() }
    val arr2 = UIntArray(10) { (it / 2).toUInt() }
    return if (arr1 == arr2) "FAIL" else "OK"
}
