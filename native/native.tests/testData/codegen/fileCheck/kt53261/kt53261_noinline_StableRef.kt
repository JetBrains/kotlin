// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.StableRef#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)

// CHECK-OPT: call i8* @"kfun:kotlinx.cinterop#<StableRef-unbox>(kotlin.Any?){}kotlinx.cinterop.StableRef<-1:0>?"

// CHECK-OPT-LABEL: epilogue:

// This test is useless in debug mode.
// TODO(KT-59288): add ability to ignore tests in debug mode
// CHECK-DEBUG-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String {
    val ref1 = StableRef.create(Any())
    val ref2 = StableRef.create(Any())
    val isEqual = ref1 == ref2
    ref2.dispose()
    ref1.dispose()
    return if (!isEqual) "OK" else "FAIL"
}
