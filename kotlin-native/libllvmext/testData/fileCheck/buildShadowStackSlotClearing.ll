; OPT: --passes=kotlin-build-shadow-stack<clear-dead-slots>,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare ptr @Kotlin_gc_returnSlot()
declare void @safepoint_1()
declare void @safepoint_2()
declare void @safepoint_3()
declare ptr addrspace(1) @alloc_func()
declare void @use_func(ptr addrspace(1))
declare void @use_plain(ptr)
declare ptr addrspace(1) @getter(ptr)

; CHECK-LABEL: define void @dead_root_cleared()
; CHECK: %a = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %a, ptr %slot_0
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: call void @use_func(ptr %a)
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_2()
; CHECK-NEXT: call void @safepoint_3()
define void @dead_root_cleared() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @safepoint_2()
  call void @safepoint_3()
  ret void
}

; CHECK-LABEL: define void @cleared_at_join(i1 %c)
; CHECK: join:
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_2()
define void @cleared_at_join(i1 %c) #0 {
entry:
  br i1 %c, label %then, label %join

then:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  br label %join

join:
  call void @safepoint_2()
  ret void
}

; CHECK-LABEL: define void @loop_carried(i32 %n)
; CHECK: store ptr %a, ptr %slot_0
; CHECK: loop:
; CHECK-NOT: store ptr null
; CHECK: exit:
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_2()
define void @loop_carried(i32 %n) #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  br label %loop

loop:
  %i = phi i32 [ 0, %entry ], [ %i.next, %loop ]
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  %i.next = add i32 %i, 1
  %done = icmp eq i32 %i.next, %n
  br i1 %done, label %exit, label %loop

exit:
  call void @safepoint_2()
  ret void
}

; CHECK-LABEL: define void @shared_slot()
; CHECK: alloca [3 x ptr]
; CHECK: call void @use_func(ptr %a)
; CHECK-NEXT: %[[CLEAR1:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR1]]
; CHECK-NEXT: %b = call ptr @alloc_func()
; CHECK-NEXT: %[[SLOT_B:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %b, ptr %[[SLOT_B]]
; CHECK-NEXT: call void @safepoint_2()
; CHECK-NEXT: call void @use_func(ptr %b)
; CHECK-NEXT: %[[CLEAR2:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR2]]
; CHECK-NEXT: call void @safepoint_3()
define void @shared_slot() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  %b = call ptr addrspace(1) @alloc_func()
  call void @safepoint_2()
  call void @use_func(ptr addrspace(1) %b)
  call void @safepoint_3()
  ret void
}

; CHECK-LABEL: define void @return_slot_read_back()
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @return_slot_read_back() #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %r = call ptr addrspace(1) @getter(ptr %slot)
  %v = load ptr, ptr %slot
  call void @safepoint_1()
  call void @use_plain(ptr %v)
  ret void
}

; CHECK-LABEL: define void @return_slot_written_through_phi(i1 %c, ptr %p)
; CHECK: join:
; CHECK: store ptr %p, ptr %s
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_1()
define void @return_slot_written_through_phi(i1 %c, ptr %p) #0 {
entry:
  %s1 = call ptr @Kotlin_gc_returnSlot()
  %s2 = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %a, label %b
a:
  %r1 = call ptr addrspace(1) @getter(ptr %s1)
  br label %join
b:
  %r2 = call ptr addrspace(1) @getter(ptr %s2)
  br label %join
join:
  %s = phi ptr [ %s1, %a ], [ %s2, %b ]
  store ptr %p, ptr %s
  call void @safepoint_1()
  ret void
}

; CHECK-LABEL: define void @return_slot_phi_with_other_pointer(i1 %c, ptr %p)
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @return_slot_phi_with_other_pointer(i1 %c, ptr %p) #0 {
entry:
  %s1 = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %a, label %join
a:
  %r1 = call ptr addrspace(1) @getter(ptr %s1)
  br label %join
join:
  %s = phi ptr [ %s1, %a ], [ %p, %entry ]
  %v = load ptr, ptr %s
  call void @safepoint_1()
  call void @use_plain(ptr %v)
  ret void
}
