// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-AAPCS-EAGER_SHADOW_STACK-LABEL: define i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-AAPCS-LATE_SHADOW_STACK-LABEL: define i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)
// CHECK-DEFAULTABI-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)
// CHECK-WINDOWSX64-EAGER_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-LATE_SHADOW_STACK-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr addrspace(1) %0, ptr addrspace(1) %1)
// CHECK-EAGER_SHADOW_STACK: call ptr @"kfun:#<C-unbox>(kotlin.Any?){}C?"
// CHECK-LATE_SHADOW_STACK: call ptr addrspace(1) @"kfun:#<C-unbox>(kotlin.Any?){}C?"
value class C(val x: Any)
// Note: <C-unbox> is also called from bridges for equals, hashCode and toString.

fun box() =
    if (C(42) == C(13)) "FAIL" else "OK"
