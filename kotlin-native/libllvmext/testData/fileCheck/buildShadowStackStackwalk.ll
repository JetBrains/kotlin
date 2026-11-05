; OPT: --passes=kotlin-build-shadow-stack<stackwalk>,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

; CHECK-NOT: addrspace(1)
; CHECK: declare ptr @alloc_func()
; CHECK: declare void @use_func(ptr)
; CHECK: declare ptr @uncalled_decl()
; CHECK-NOT: addrspace(1)

declare void @Kotlin_mm_safePointFunctionPrologue()
declare void @safepoint_1()
declare void @safepoint_2()
declare ptr addrspace(1) @alloc_func()
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @uncalled_decl()
declare void @throwing_call()
declare i32 @__gxx_personality_v0(...)

; CHECK-LABEL: define ptr @leaf_function(ptr %arg)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: call void @llvm.memset
; CHECK-NOT: call void @EnterFrame
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret ptr %arg
define ptr addrspace(1) @leaf_function(ptr addrspace(1) %arg) #0 {
entry:
  ret ptr addrspace(1) %arg
}

; CHECK-LABEL: define i32 @root_dead_before_safepoint()
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: call void @EnterFrame
; CHECK: %obj = call ptr @alloc_func()
; CHECK: call void @safepoint_1()
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret i32 %val
define i32 @root_dead_before_safepoint() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %val = load i32, ptr addrspace(1) %obj
  call void @safepoint_1()
  ret i32 %val
}

; CHECK-LABEL: define i32 @caller_rooted_arg(ptr %arg)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: call void @EnterFrame
; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
; CHECK: call void @safepoint_1()
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret i32 %val
define i32 @caller_rooted_arg(ptr addrspace(1) %arg) #0 {
entry:
  call void @Kotlin_mm_safePointFunctionPrologue()
  call void @safepoint_1()
  %val = load i32, ptr addrspace(1) %arg
  ret i32 %val
}

; CHECK-LABEL: define i32 @root_live_across_safepoint()
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [1 x ptr], align 8
; CHECK: call void @llvm.memset{{.*}}%shadow_stack_frame
; CHECK-NEXT: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 1, i32 0, ptr %shadow_stack_frame)
; CHECK-NOT: call void @EnterFrame
; CHECK: %obj = call ptr @alloc_func()
; CHECK: %slot_0 = getelementptr inbounds [1 x ptr], ptr %shadow_stack_frame, i32 0, i32 0
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret i32 %val
define i32 @root_live_across_safepoint() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  %val = load i32, ptr addrspace(1) %obj
  ret i32 %val
}

; CHECK-LABEL: define void @slot_sharing()
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [1 x ptr], align 8
; CHECK: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 1, i32 0, ptr %shadow_stack_frame)
; CHECK: %slot_0 = getelementptr inbounds [1 x ptr], ptr %shadow_stack_frame, i32 0, i32 0
; CHECK: store ptr %obj1, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: %slot_01 = getelementptr inbounds [1 x ptr], ptr %shadow_stack_frame, i32 0, i32 0
; CHECK: store ptr %obj2, ptr %slot_01
; CHECK: call void @safepoint_2()
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret void
define void @slot_sharing() #0 {
entry:
  %obj1 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj1)

  %obj2 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_2()
  call void @use_func(ptr addrspace(1) %obj2)

  ret void
}

; CHECK-LABEL: define void @two_simultaneous_roots()
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [2 x ptr], align 8
; CHECK: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 2, i32 0, ptr %shadow_stack_frame)
; CHECK: %slot_0 = getelementptr inbounds [2 x ptr], ptr %shadow_stack_frame, i32 0, i32 0
; CHECK: store ptr %obj1, ptr %slot_0
; CHECK: %slot_1 = getelementptr inbounds [2 x ptr], ptr %shadow_stack_frame, i32 0, i32 1
; CHECK: store ptr %obj2, ptr %slot_1
; CHECK: call void @safepoint_1()
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret void
define void @two_simultaneous_roots() #0 {
entry:
  %obj1 = call ptr addrspace(1) @alloc_func()
  %obj2 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj1)
  call void @use_func(ptr addrspace(1) %obj2)
  ret void
}

; CHECK-LABEL: define void @exception_unwinding()
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [1 x ptr], align 8
; CHECK: call void (i64, i32, ...) @llvm.experimental.stackmap(i64 1, i32 0, ptr %shadow_stack_frame)
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: invoke void @throwing_call()
; CHECK:   to label %normal unwind label %lpad
; CHECK: normal:
; CHECK-NOT: call void @LeaveFrame
; CHECK: ret void
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK:   cleanup
; CHECK-NOT: call void @LeaveFrame
; CHECK: resume { ptr, i32 } %lp
define void @exception_unwinding() #0 personality ptr @__gxx_personality_v0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  invoke void @throwing_call()
    to label %normal unwind label %lpad

normal:
  call void @use_func(ptr addrspace(1) %obj)
  ret void

lpad:
  %lp = landingpad { ptr, i32 }
          cleanup
  resume { ptr, i32 } %lp
}
