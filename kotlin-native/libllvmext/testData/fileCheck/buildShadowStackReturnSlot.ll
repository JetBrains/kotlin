; OPT: --passes=kotlin-build-shadow-stack<clear-dead-slots>,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare ptr @Kotlin_gc_returnSlot()
declare void @safepoint_1()
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @produce()
declare ptr addrspace(1) @getter_no_args(ptr)
declare ptr addrspace(1) @getter_one_arg(ptr addrspace(1), ptr)
declare ptr @raw_allocate(i64)
declare ptr @plain_getter(ptr)
declare void @use_plain(ptr)

; CHECK-LABEL: define ptr @adapter_like()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: %res = call ptr @getter_no_args(ptr %slot_0)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret ptr %res
define ptr addrspace(1) @adapter_like() #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %res = call ptr addrspace(1) @getter_no_args(ptr %slot)
  ret ptr addrspace(1) %res
}

; CHECK-LABEL: define void @root_then_marker()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: %obj = call ptr @produce()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: call ptr @getter_no_args(ptr %slot_1)
define void @root_then_marker() #0 {
entry:
  %obj = call ptr addrspace(1) @produce()
  call void @safepoint_1()
  %slot = call ptr @Kotlin_gc_returnSlot()
  %res = call ptr addrspace(1) @getter_no_args(ptr %slot)
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

; CHECK-LABEL: define void @two_getter_calls()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: %r1 = call ptr @getter_no_args(ptr %slot_1)
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %r1, ptr %slot_0
; CHECK: %r2 = call ptr @getter_no_args(ptr %slot_1)
define void @two_getter_calls() #0 {
entry:
  %slot1 = call ptr @Kotlin_gc_returnSlot()
  %r1 = call ptr addrspace(1) @getter_no_args(ptr %slot1)
  %slot2 = call ptr @Kotlin_gc_returnSlot()
  %r2 = call ptr addrspace(1) @getter_no_args(ptr %slot2)
  call void @use_func(ptr addrspace(1) %r1)
  ret void
}

; CHECK-LABEL: define i32 @inlined_getter_result()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: %obj = getelementptr inbounds i8, ptr %cell, i64 8
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK-NEXT: store ptr %obj, ptr %slot_1
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: %cell2 = call ptr @raw_allocate(i64 32)
; CHECK-NEXT: %obj2 = getelementptr inbounds i8, ptr %cell2, i64 8
; CHECK-NEXT: store ptr %obj2, ptr %slot_1
; CHECK: load i32, ptr %count
define i32 @inlined_getter_result() #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %cell = call ptr @raw_allocate(i64 32)
  %obj = getelementptr inbounds i8, ptr %cell, i64 8
  store ptr %obj, ptr %slot
  %slot2 = call ptr @Kotlin_gc_returnSlot()
  %cell2 = call ptr @raw_allocate(i64 32)
  %obj2 = getelementptr inbounds i8, ptr %cell2, i64 8
  store ptr %obj2, ptr %slot2
  %count = getelementptr inbounds i8, ptr %cell, i64 16
  %v = load i32, ptr %count
  ret i32 %v
}

; CHECK-LABEL: define void @inlined_getter_reference_covered()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: %obj = getelementptr inbounds i8, ptr %cell, i64 8
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK-NEXT: store ptr %obj, ptr %slot_1
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: call void @use_func(ptr %obj)
define void @inlined_getter_reference_covered() #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %cell = call ptr @raw_allocate(i64 32)
  %obj = getelementptr inbounds i8, ptr %cell, i64 8
  store ptr %obj, ptr %slot
  %ref = addrspacecast ptr %obj to ptr addrspace(1)
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %ref)
  ret void
}

; CHECK-LABEL: define void @plain_getter_result()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: %obj = call ptr @plain_getter(ptr %slot_1)
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: call void @use_plain(ptr %obj)
define void @plain_getter_result() #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %obj = call ptr @plain_getter(ptr %slot)
  %slot2 = call ptr @Kotlin_gc_returnSlot()
  %obj2 = call ptr @plain_getter(ptr %slot2)
  call void @safepoint_1()
  call void @use_plain(ptr %obj)
  ret void
}

; CHECK-NOT: @Kotlin_gc_returnSlot
; CHECK-NOT: addrspace(1)
