; OPT: --passes=kotlin-build-shadow-stack,verify
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
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @llvm.memset{{.*}}%shadow_stack_frame
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %obj = call ptr @alloc_func()
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
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
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %obj1, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: %slot_01 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %obj2, ptr %slot_01
; CHECK: call void @safepoint_2()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
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
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %obj1, ptr %slot_0
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: store ptr %obj2, ptr %slot_1
; CHECK: call void @safepoint_1()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
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
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: invoke void @throwing_call()
; CHECK:   to label %normal unwind label %lpad
; CHECK: normal:
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK:   cleanup
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
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

; CHECK-LABEL: define ptr @address_space_lowering(ptr %x)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: addrspacecast
; CHECK: ret ptr %x
define ptr @address_space_lowering(ptr addrspace(1) %x) #0 {
entry:
  %cast = addrspacecast ptr addrspace(1) %x to ptr
  ret ptr %cast
}

; CHECK-LABEL: define i32 @loop_dead_before_safepoint(ptr %field)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: loop:
; CHECK: call void @safepoint_1()
; CHECK: br label %loop
define i32 @loop_dead_before_safepoint(ptr %field) #0 {
entry:
  br label %loop

loop:
  %obj = load ptr addrspace(1), ptr %field
  %val = load i32, ptr addrspace(1) %obj
  call void @safepoint_1()
  br label %loop
}

; CHECK-LABEL: define i32 @loop_dead_with_exit(ptr %field, i32 %n)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK: loop:
; CHECK: call void @safepoint_1()
; CHECK-NOT: LeaveFrame
; CHECK: ret i32 %val
define i32 @loop_dead_with_exit(ptr %field, i32 %n) #0 {
entry:
  br label %loop

loop:
  %i = phi i32 [ 0, %entry ], [ %next, %loop ]
  %obj = load ptr addrspace(1), ptr %field
  %val = load i32, ptr addrspace(1) %obj
  call void @safepoint_1()
  %next = add i32 %i, 1
  %cond = icmp slt i32 %next, %n
  br i1 %cond, label %loop, label %exit

exit:
  ret i32 %val
}

; CHECK-LABEL: define ptr @null_lowering_ssa(i1 %cond, ptr %arg)
; CHECK-NOT: addrspace(1)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK: merge:
; CHECK: %res = phi ptr [ %arg, %then ], [ null, %else ]
; CHECK: %sel = select i1 %cond, ptr %res, ptr null
; CHECK: %cmp = icmp eq ptr %sel, null
; CHECK: ret ptr %sel
define ptr addrspace(1) @null_lowering_ssa(i1 %cond, ptr addrspace(1) %arg) #0 {
entry:
  br i1 %cond, label %then, label %else

then:
  br label %merge

else:
  br label %merge

merge:
  %res = phi ptr addrspace(1) [ %arg, %then ], [ null, %else ]
  %sel = select i1 %cond, ptr addrspace(1) %res, ptr addrspace(1) null
  %cmp = icmp eq ptr addrspace(1) %sel, null
  ret ptr addrspace(1) %sel
}

; CHECK-LABEL: define void @null_store_and_call(ptr %slot)
; CHECK-NOT: addrspace(1)
; CHECK: store ptr null, ptr %slot
; CHECK: call void @use_func(ptr null)
; CHECK: ret void
define void @null_store_and_call(ptr %slot) #0 {
entry:
  store ptr addrspace(1) null, ptr %slot
  call void @use_func(ptr addrspace(1) null)
  ret void
}

; CHECK-LABEL: define void @loop_carried_root_minimal_slots(i32 %n)
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: loop:
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %cur)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @loop_carried_root_minimal_slots(i32 %n) #0 {
entry:
  %init = call ptr addrspace(1) @alloc_func()
  br label %loop

loop:
  %cur = phi ptr addrspace(1) [ %init, %entry ], [ %next, %loop ]
  %i = phi i32 [ 0, %entry ], [ %i_next, %loop ]
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %cur)
  %next = call ptr addrspace(1) @alloc_func()
  %i_next = add i32 %i, 1
  %cond = icmp slt i32 %i_next, %n
  br i1 %cond, label %loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @dead_root_not_cleared_by_default()
; CHECK: store ptr %a, ptr %slot_0
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @dead_root_not_cleared_by_default() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @safepoint_2()
  ret void
}
