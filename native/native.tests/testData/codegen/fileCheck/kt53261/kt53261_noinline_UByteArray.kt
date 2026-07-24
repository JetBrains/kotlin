// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlin.UByteArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UByteArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UByteArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"

// CHECK-OPT: call ptr @"kfun:kotlin#<UByteArray-unbox>(kotlin.Any?){}kotlin.UByteArray?"

// CHECK-LABEL: epilogue:

fun box(): String {
    val arr1 = UByteArray(10) { it.toUByte() }
    val arr2 = UByteArray(10) { (it / 2).toUByte() }
    return if (arr1 == arr2) "FAIL" else "OK"
}
