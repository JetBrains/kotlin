; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @Kotlin_gc_stackObject(ptr)
declare ptr @Kotlin_gc_returnSlot()
declare void @safepoint_1()
declare void @raw_call(ptr)
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @produce()
declare ptr addrspace(1) @getter_no_args(ptr)
declare void @llvm.lifetime.start.p0(i64, ptr)
declare void @llvm.lifetime.end.p0(i64, ptr)

; CHECK-LABEL: define void @stack_object_without_reference(ptr %value)
; CHECK: %obj = alloca [3 x ptr], align 8
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK-NOT: Kotlin_gc_stackObject
; CHECK: store ptr %value, ptr %field
; CHECK: call void @safepoint_1()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
define void @stack_object_without_reference(ptr addrspace(1) %value) #0 {
entry:
  %obj = alloca [3 x ptr], align 8
  call void @Kotlin_gc_stackObject(ptr %obj)
  %field = getelementptr inbounds i8, ptr %obj, i64 16
  store ptr addrspace(1) %value, ptr %field
  call void @safepoint_1()
  call void @raw_call(ptr %field)
  ret void
}

; CHECK-LABEL: define void @stack_object_lifetime(i64 %n)
; CHECK: loop:
; CHECK: call void @llvm.lifetime.start.p0(i64 24, ptr %obj)
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: [[SLOT:%slot_0[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr [[SLOT]]
; CHECK-NEXT: call void @llvm.lifetime.end.p0(i64 24, ptr %obj)
define void @stack_object_lifetime(i64 %n) #0 {
entry:
  %obj = alloca [3 x ptr], align 8
  br label %loop

loop:
  %i = phi i64 [ 0, %entry ], [ %i.next, %loop ]
  call void @llvm.lifetime.start.p0(i64 24, ptr %obj)
  call void @Kotlin_gc_stackObject(ptr %obj)
  call void @safepoint_1()
  call void @llvm.lifetime.end.p0(i64 24, ptr %obj)
  %i.next = add i64 %i, 1
  %done = icmp eq i64 %i.next, %n
  br i1 %done, label %exit, label %loop

exit:
  ret void
}

; CHECK-LABEL: define void @duplicated_marker(i1 %c)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: a:
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: b:
; CHECK-NEXT: [[SLOT:%slot_0[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr [[SLOT]]
define void @duplicated_marker(i1 %c) #0 {
entry:
  %obj = alloca [3 x ptr], align 8
  br i1 %c, label %a, label %b

a:
  call void @Kotlin_gc_stackObject(ptr %obj)
  br label %join

b:
  call void @Kotlin_gc_stackObject(ptr %obj)
  br label %join

join:
  call void @safepoint_1()
  ret void
}

; CHECK-LABEL: define void @slots_after_roots_and_return_slot()
; CHECK: %shadow_stack_frame = alloca [6 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 6)
; CHECK: %slot_1 = getelementptr inbounds [6 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: %obj = call ptr @produce()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [6 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK-NEXT: %slot_2 = getelementptr inbounds [6 x ptr], ptr %shadow_stack_frame, i32 0, i32 4
; CHECK-NEXT: store ptr %a, ptr %slot_2
; CHECK-NEXT: %slot_3 = getelementptr inbounds [6 x ptr], ptr %shadow_stack_frame, i32 0, i32 5
; CHECK-NEXT: store ptr %b, ptr %slot_3
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: call ptr @getter_no_args(ptr %slot_1)
define void @slots_after_roots_and_return_slot() #0 {
entry:
  %a = alloca [2 x ptr], align 8
  %b = alloca [2 x ptr], align 8
  %obj = call ptr addrspace(1) @produce()
  call void @Kotlin_gc_stackObject(ptr %a)
  call void @Kotlin_gc_stackObject(ptr %b)
  call void @safepoint_1()
  %slot = call ptr @Kotlin_gc_returnSlot()
  %res = call ptr addrspace(1) @getter_no_args(ptr %slot)
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

; CHECK-NOT: Kotlin_gc_stackObject
