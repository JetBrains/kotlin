// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-EAGER_SHADOW_STACK-LABEL: define i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-AAPCS-OPT-LATE_SHADOW_STACK-LABEL: define i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr addrspace(1) %1)
// CHECK-DEFAULTABI-OPT-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr addrspace(1) %1)
// CHECK-WINDOWSX64-OPT-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr addrspace(1) %1)
// CHECK-OPT: call ptr @"kfun:kotlinx.cinterop#<CPointer-unbox>(kotlin.Any?){}kotlinx.cinterop.CPointer<-1:0>?"

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    return if (var1.ptr == var2.ptr) "FAIL" else "OK"
}
