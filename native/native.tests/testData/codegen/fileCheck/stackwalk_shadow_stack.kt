// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: BuildShadowStack
// FREE_COMPILER_ARGS: -Xbinary=stackwalkShadowStack=true

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)

class SimpleData(val value: Int, var next: SimpleData? = null)

// CHECK-DEBUG-LABEL: define {{.*}}i32 @"kfun:#leafComputation(kotlin.Int;kotlin.Int){}kotlin.Int"(i32 %0, i32 %1)
// CHECK-DEBUG-NOT: EnterFrame
// CHECK-DEBUG-NOT: LeaveFrame
// CHECK-DEBUG-NOT: llvm.experimental.stackmap
fun leafComputation(a: Int, b: Int): Int {
    return (a * 31) xor (b + 17)
}

// CHECK-LABEL: define {{.*}}ptr @"kfun:#allocatingFunction(kotlin.Int){}SimpleData"(i32 {{.*}}%0)
// CHECK: call void @llvm.memset
// CHECK-NEXT: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 {{[1-9][0-9]*}}, i32 0, ptr %shadow_stack_frame)
// CHECK-NOT: EnterFrame
// CHECK-NOT: LeaveFrame
// CHECK: ret
fun allocatingFunction(n: Int): SimpleData {
    val head = SimpleData(n)
    var curr = head
    for (i in 1..5) {
        val next = SimpleData(n + i)
        curr.next = next
        curr = next
    }
    return head
}

// CHECK-LABEL: define {{.*}}ptr @"kfun:#exceptionUnwindFunction(kotlin.Boolean){}kotlin.String"(i1 {{.*}}%0)
// CHECK: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 {{[1-9][0-9]*}}, i32 0, ptr %shadow_stack_frame)
// CHECK-NOT: EnterFrame
// CHECK: landingpad
// CHECK-NOT: SetCurrentFrame
// CHECK-NOT: LeaveFrame
// CHECK: ret
fun exceptionUnwindFunction(shouldThrow: Boolean): String {
    val root = SimpleData(42)
    try {
        if (shouldThrow) {
            throw IllegalArgumentException("Test exception from stack frame")
        }
        return "success_${root.value}"
    } catch (e: IllegalArgumentException) {
        return "caught_${root.value}"
    }
}

fun chainedAllocations(): Int {
    val d1 = allocatingFunction(10)
    val d2 = allocatingFunction(20)
    return d1.value + d2.value
}

fun box(): String {
    val leafRes = leafComputation(10, 20)
    if (leafRes != ((10 * 31) xor (20 + 17))) {
        return "FAIL leafComputation: $leafRes"
    }

    val chain = allocatingFunction(100)
    val next = chain.next
    if (chain.value != 100 || next == null || next.value != 101) {
        return "FAIL allocatingFunction"
    }

    val unwindCatch = exceptionUnwindFunction(true)
    if (unwindCatch != "caught_42") {
        return "FAIL exceptionUnwind caught: $unwindCatch"
    }

    val unwindSuccess = exceptionUnwindFunction(false)
    if (unwindSuccess != "success_42") {
        return "FAIL exceptionUnwind success: $unwindSuccess"
    }

    val sum = chainedAllocations()
    if (sum != 30) {
        return "FAIL chainedAllocations: $sum"
    }

    return "OK"
}
