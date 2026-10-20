; OPT: --passes=kotlin-build-shadow-stack<clear-dead-slots=late>,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @safepoint_1()
declare void @safepoint_2()
declare ptr addrspace(1) @alloc_func()
declare void @use_func(ptr addrspace(1))
declare i32 @__gxx_personality_v0(...)
declare void @llvm.sideeffect()
declare void @Kotlin_gc_frameSetCurrent()

; CHECK-LABEL: define void @cleared_at_callee_exit()
; CHECK: call void @use_func(ptr %a)
; CHECK-NEXT: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK-NEXT: call void @safepoint_2()
define void @cleared_at_callee_exit() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_2()
  ret void
}

; CHECK-LABEL: define void @not_cleared_at_safepoint()
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @not_cleared_at_safepoint() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @safepoint_2()
  ret void
}

; CHECK-LABEL: define void @live_root_kept_at_callee_exit()
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @live_root_kept_at_callee_exit() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  call void @safepoint_2()
  call void @use_func(ptr addrspace(1) %a)
  ret void
}

; CHECK-LABEL: define void @not_cleared_before_return()
; CHECK-NOT: store ptr null
; CHECK-NOT: call void @llvm.sideeffect()
; CHECK: ret void
define void @not_cleared_before_return() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @llvm.sideeffect(), !kotlin.gc.callee_exit !0
  ret void
}

; CHECK-LABEL: define void @not_cleared_at_untagged_sideeffect()
; CHECK-NOT: store ptr null
; CHECK: call void @llvm.sideeffect()
; CHECK-NOT: store ptr null
; CHECK: ret void
define void @not_cleared_at_untagged_sideeffect() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  call void @llvm.sideeffect()
  call void @safepoint_2()
  ret void
}

; CHECK-LABEL: define void @cleared_at_landing_pad()
; CHECK: lpad:
; CHECK: %[[CLEAR:slot_[0-9]+]] = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr null, ptr %[[CLEAR]]
; CHECK: call void @safepoint_2()
define void @cleared_at_landing_pad() #0 personality ptr @__gxx_personality_v0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  invoke void @use_func(ptr addrspace(1) %a)
          to label %normal unwind label %lpad

normal:
  ret void

lpad:
  %lp = landingpad { ptr, i32 }
          catch ptr null
  call void @Kotlin_gc_frameSetCurrent()
  call void @safepoint_2()
  ret void
}

!0 = !{}
