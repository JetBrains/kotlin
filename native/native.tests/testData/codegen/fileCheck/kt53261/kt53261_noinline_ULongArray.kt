// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

// CHECK-AAPCS-OPT-EAGER_SHADOW_STACK-LABEL: define i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-AAPCS-OPT-LATE_SHADOW_STACK-LABEL: define i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)
// CHECK-DEFAULTABI-OPT-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)
// CHECK-WINDOWSX64-OPT-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlin.ULongArray#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"

// CHECK-OPT-EAGER_SHADOW_STACK: call ptr @"kfun:kotlin#<ULongArray-unbox>(kotlin.Any?){}kotlin.ULongArray?"
// CHECK-OPT-LATE_SHADOW_STACK: call ptr addrspace(1) @"kfun:kotlin#<ULongArray-unbox>(kotlin.Any?){}kotlin.ULongArray?"

// CHECK-LABEL: epilogue:

fun box(): String {
    val arr1 = ULongArray(10) { it.toULong() }
    val arr2 = ULongArray(10) { (it / 2).toULong() }
    return if (arr1 == arr2) "FAIL" else "OK"
}
