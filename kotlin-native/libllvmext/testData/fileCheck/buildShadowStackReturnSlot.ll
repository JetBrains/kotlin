; OPT: --passes=kotlin-build-shadow-stack,verify
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
declare void @llvm.sideeffect()

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
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  %slot2 = call ptr @Kotlin_gc_returnSlot()
  %cell2 = call ptr @raw_allocate(i64 32)
  %obj2 = getelementptr inbounds i8, ptr %cell2, i64 8
  store ptr %obj2, ptr %slot2
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
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
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
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

; CHECK-LABEL: define void @plain_root_through_slot_phi(i1 %c)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: join:
; CHECK-NEXT: %s = phi ptr [ %slot_1, %a ], [ %slot_1, %b ]
; CHECK-NEXT: %obj = call ptr @plain_getter(ptr %s)
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK-NEXT: call void @use_plain(ptr %obj)
define void @plain_root_through_slot_phi(i1 %c) #0 {
entry:
  %s1 = call ptr @Kotlin_gc_returnSlot()
  %s2 = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %a, label %b
a:
  br label %join
b:
  br label %join
join:
  %s = phi ptr [ %s1, %a ], [ %s2, %b ]
  %obj = call ptr @plain_getter(ptr %s)
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_1()
  call void @use_plain(ptr %obj)
  ret void
}

; CHECK-LABEL: define void @phi_value_stored_to_return_slot(i1 %c)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: join:
; CHECK-NEXT: %obj.base = phi ptr [ %obj1, %a ], [ %obj2, %b ]
; CHECK-NEXT: %obj = phi ptr [ %obj1, %a ], [ %obj2, %b ]
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj.base, ptr %slot_0
; CHECK-NEXT: store ptr %obj, ptr %slot_1
define void @phi_value_stored_to_return_slot(i1 %c) #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %a, label %b
a:
  %cell1 = call ptr @raw_allocate(i64 32)
  %obj1 = getelementptr inbounds i8, ptr %cell1, i64 8
  br label %join
b:
  %cell2 = call ptr @raw_allocate(i64 32)
  %obj2 = getelementptr inbounds i8, ptr %cell2, i64 8
  br label %join
join:
  %obj = phi ptr [ %obj1, %a ], [ %obj2, %b ]
  store ptr %obj, ptr %slot
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_1()
  call void @use_plain(ptr %obj)
  ret void
}

; CHECK-LABEL: define void @phi_of_reference_and_plain_stored_to_return_slot(ptr %thiz, i1 %c)
; CHECK: merge:
; CHECK-NEXT: %merged.base = phi ptr [ %thiz, %early_ret ], [ %new_obj, %alloc_ret ]
; CHECK-NEXT: %merged = phi ptr [ %thiz, %early_ret ], [ %new_obj, %alloc_ret ]
; CHECK-NEXT: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %merged.base, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK-NEXT: call void @use_func(ptr %merged)
define void @phi_of_reference_and_plain_stored_to_return_slot(ptr addrspace(1) %thiz, i1 %c) #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %thiz_raw = addrspacecast ptr addrspace(1) %thiz to ptr
  br i1 %c, label %early_ret, label %alloc_ret
early_ret:
  br label %merge
alloc_ret:
  %new_obj = call ptr @raw_allocate(i64 32)
  br label %merge
merge:
  %merged = phi ptr [ %thiz_raw, %early_ret ], [ %new_obj, %alloc_ret ]
  store ptr %merged, ptr %slot
  %res = addrspacecast ptr %merged to ptr addrspace(1)
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %res)
  ret void
}

; CHECK-LABEL: define i32 @phi_stored_to_return_slot_sibling_field_used(i1 %c)
; CHECK: merge:
; CHECK: %p.base = phi ptr [ %obj1, %a ], [ %obj2, %b ]
; CHECK: store ptr %p.base, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK-NEXT: %v = load i32, ptr %p
define i32 @phi_stored_to_return_slot_sibling_field_used(i1 %c) #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %a, label %b
a:
  %cell1 = call ptr @raw_allocate(i64 32)
  %obj1 = getelementptr inbounds i8, ptr %cell1, i64 8
  %count1 = getelementptr inbounds i8, ptr %cell1, i64 16
  br label %merge
b:
  %cell2 = call ptr @raw_allocate(i64 32)
  %obj2 = getelementptr inbounds i8, ptr %cell2, i64 8
  %count2 = getelementptr inbounds i8, ptr %cell2, i64 16
  br label %merge
merge:
  %obj = phi ptr [ %obj1, %a ], [ %obj2, %b ]
  %p = phi ptr [ %count1, %a ], [ %count2, %b ]
  store ptr %obj, ptr %slot
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_1()
  %v = load i32, ptr %p
  ret i32 %v
}

; CHECK-NOT: @Kotlin_gc_returnSlot
; CHECK-NOT: addrspace(1)

!0 = !{}
