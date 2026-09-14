// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: RemoveRedundantSafepoints
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

// While the optimizer runs, object references must live in a non-integral address space, so that it cannot
// turn them into integers and hide them from the late shadow stack pass.
// CHECK: target datalayout = "{{.*}}-ni:1"
// CHECK: ptr addrspace(1)

fun box(): String = "OK"
