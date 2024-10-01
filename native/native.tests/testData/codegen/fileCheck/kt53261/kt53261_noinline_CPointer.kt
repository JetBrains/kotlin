// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-OPT: call ptr @"kfun:kotlinx.cinterop#<CPointer-unbox>(kotlin.Any?){}kotlinx.cinterop.CPointer<-1:0>?"

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    return if (var1.ptr == var2.ptr) "FAIL" else "OK"
}
