; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p:32:32-p270:32:32-p271:32:32-p272:64:64-i128:128-f64:32:64-f80:32-n8:16:32-S128"
target triple = "i686-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare ptr addrspace(1) @alloc_func()
declare void @safepoint_1()
declare void @use_func(ptr addrspace(1))

; CHECK-LABEL: define void @frame_on_32_bit_target()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK-NEXT: store ptr %a, ptr %slot_0
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
define void @frame_on_32_bit_target() #0 {
entry:
  %a = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %a)
  ret void
}
