// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: BuildShadowStack
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)

class SimpleData(val value: Int, var next: SimpleData? = null)

// CHECK-LABEL: define {{.*}}i32 @"kfun:#leafComputation(kotlin.Int;kotlin.Int){}kotlin.Int"(i32 %0, i32 %1)
// CHECK-NOT: EnterFrame
// CHECK-NOT: LeaveFrame
fun leafComputation(a: Int, b: Int): Int {
    return (a * 31) xor (b + 17)
}

// CHECK-LABEL: define {{.*}}ptr @"kfun:#allocatingFunction(kotlin.Int){}SimpleData"(i32 %0, ptr{{.*}}%1)
// CHECK: call void @llvm.memset
// CHECK-DEBUG: call void @EnterFrame(ptr %{{.*}}, i32 0, i32 {{.*}})
// CHECK-DEBUG: call void @LeaveFrame(ptr %{{.*}}, i32 0, i32 {{.*}})
// CHECK-OPT-NOT: call void @EnterFrame
// CHECK-OPT: store ptr %shadow_stack_frame, ptr
// CHECK-OPT: store i32 0, ptr
// CHECK-OPT: store i32 {{[1-9][0-9]*}}, ptr
// CHECK-OPT-NOT: call void @LeaveFrame
// CHECK-OPT: load ptr, ptr %shadow_stack_frame
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

// CHECK-LABEL: define {{.*}}ptr @"kfun:#exceptionUnwindFunction(kotlin.Boolean){}kotlin.String"(i1 {{.*}}%0, ptr{{.*}}%1)
// CHECK-DEBUG: call void @EnterFrame(ptr %{{.*}}, i32 0, i32 {{.*}})
// CHECK-OPT: store ptr %shadow_stack_frame, ptr
// CHECK: landingpad
// CHECK-DEBUG: call void @SetCurrentFrame(ptr %{{.*}})
// CHECK-DEBUG: call void @LeaveFrame(ptr %{{.*}}, i32 0, i32 {{.*}})
// CHECK-OPT-NOT: call void @SetCurrentFrame
// CHECK-OPT: store ptr %shadow_stack_frame, ptr
// CHECK-OPT-NOT: call void @LeaveFrame
// CHECK-OPT: load ptr, ptr %shadow_stack_frame
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
