// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)

// CHECK-OPT: call ptr @"kfun:kotlinx.cinterop#<StableRef-unbox>(kotlin.Any?){}kotlinx.cinterop.StableRef<-1:0>?"

// CHECK-OPT-LABEL: epilogue:

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String {
    val ref1 = StableRef.create(Any())
    val ref2 = StableRef.create(Any())
    val isEqual = ref1 == ref2
    ref2.dispose()
    ref1.dispose()
    return if (!isEqual) "OK" else "FAIL"
}
