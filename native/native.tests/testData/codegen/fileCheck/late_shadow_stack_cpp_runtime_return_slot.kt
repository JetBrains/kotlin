// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.internal.ExportForCppRuntime

class Box(val value: Any)

// CHECK-LABEL: define ptr addrspace(1) @Kotlin_test_wrap(ptr addrspace(1) {{.*}}%0, ptr {{.*}}%1)
// CHECK: call void @UpdateReturnRef(ptr {{.*}}%1, ptr
@ExportForCppRuntime("Kotlin_test_wrap")
fun wrap(x: Any): Any = Box(x)

// CHECK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"()
// CHECK: [[SLOT:%[0-9]+]] = call ptr @Kotlin_gc_returnSlot()
// CHECK-NEXT: call ptr addrspace(1) @Kotlin_test_wrap(ptr addrspace(1) {{.*}}, ptr {{.*}}[[SLOT]])
fun box(): String {
    val boxed = wrap("OK") as Box
    return boxed.value as String
}
