// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlinx.cinterop.*

// CHECK-OPT-LABEL: define i8* @"kfun:kotlinx.cinterop#interpretOpaquePointed(kotlin.native.internal.NativePtr){}kotlinx.cinterop.NativePointed"(i8* %0)
// CHECK-OPT: call i8* @"kfun:kotlinx.cinterop#<NativePointed-unbox>(kotlin.Any?){}kotlinx.cinterop.NativePointed?"

// This test is useless in debug mode.
// TODO(KT-59288): add ability to ignore tests in debug mode
// CHECK-DEBUG-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: NativePointed = alloc(4, 4)
    return if (interpretOpaquePointed(var1.rawPtr) is NativePointed) "OK" else "FAIL"
}
