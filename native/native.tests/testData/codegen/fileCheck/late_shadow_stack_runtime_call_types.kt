// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: RemoveRedundantSafepoints
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.Retain

class Node(var next: Node?)

// CHECK-LABEL: define {{.*}}void @"kfun:#relink(Node;Node?){}"(ptr addrspace(1) {{.*}}%0, ptr addrspace(1) {{.*}}%1)
// CHECK-NOT: call {{.*}}@UpdateHeapRef
// CHECK: store ptr
// CHECK-NOT: call {{.*}}@UpdateHeapRef
// CHECK: ret void
@Retain
fun relink(a: Node, b: Node?) {
    a.next = b
}

fun box(): String {
    val a = Node(null)
    val b = Node(null)
    relink(a, b)
    return if (a.next === b) "OK" else "FAIL"
}
