; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @throwing_call()
declare void @safepoint_1()
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @produce()
declare i32 @__gxx_personality_v0(...)
declare void @Kotlin_gc_frameSetCurrent()

; CHECK-LABEL: define void @catches_without_roots()
; CHECK: %shadow_stack_frame = alloca [2 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 2)
; CHECK: invoke void @throwing_call()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 2)
; CHECK: ret void
; CHECK: landingpad
; CHECK: cleanup
; CHECK: call void @SetCurrentFrame(ptr %shadow_stack_frame)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 2)
; CHECK: resume
define void @catches_without_roots() #0 personality ptr @__gxx_personality_v0 {
entry:
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @catch_clause_promoted_to_cleanup()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: landingpad
; CHECK: cleanup
; CHECK: call void @SetCurrentFrame(ptr %shadow_stack_frame)
define void @catch_clause_promoted_to_cleanup() #0 personality ptr @__gxx_personality_v0 {
entry:
  %obj = call ptr addrspace(1) @produce()
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } catch ptr null
  call void @Kotlin_gc_frameSetCurrent()
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @no_frame_no_repair()
; CHECK-NOT: EnterFrame
; CHECK-NOT: SetCurrentFrame
; CHECK-NOT: LeaveFrame
; CHECK: ret void
define void @no_frame_no_repair() #0 {
entry:
  call void @safepoint_1()
  ret void
}

; CHECK-LABEL: define void @unmarked_runtime_function()
; CHECK-NOT: EnterFrame
; CHECK-NOT: SetCurrentFrame
; CHECK-NOT: LeaveFrame
; CHECK-NOT: shadow_stack_frame
; CHECK: resume
define void @unmarked_runtime_function() personality ptr @__gxx_personality_v0 {
entry:
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @landingpad_phi_root()
; CHECK:      lpad:
; CHECK-NEXT:   %merged = phi ptr
; CHECK-NEXT:   %lp = landingpad
; CHECK-NEXT:     cleanup
; CHECK-NEXT:   getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame
; CHECK-NEXT:   store ptr %merged
; CHECK-NEXT:   call void @SetCurrentFrame(ptr %shadow_stack_frame)
define void @landingpad_phi_root() #0 personality ptr @__gxx_personality_v0 {
entry:
  %a = call ptr addrspace(1) @produce()
  invoke void @throwing_call() to label %second unwind label %lpad

second:
  %b = call ptr addrspace(1) @produce()
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  ret void

lpad:
  %merged = phi ptr addrspace(1) [ %a, %entry ], [ %b, %second ]
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %merged)
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @unmarked_runtime_pad()
; CHECK:      lpad:
; CHECK-NEXT:   %lp = landingpad
; CHECK-NEXT:     cleanup
; CHECK-NOT:    SetCurrentFrame
; CHECK:        call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT:   resume
define void @unmarked_runtime_pad() #0 personality ptr @__gxx_personality_v0 {
entry:
  %obj = call ptr addrspace(1) @produce()
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @throwing_call()
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @untagged_runtime_pad_calling_kotlin()
; CHECK:      runtime_lpad:
; CHECK-NEXT:   %lp = landingpad
; CHECK-NEXT:     cleanup
; CHECK-NEXT:     catch ptr @exception_type_info
; CHECK-NEXT:   %selector = extractvalue
; CHECK-NOT:    SetCurrentFrame
; CHECK:        call void @kotlin_func()
; CHECK-NEXT:   br label %done
; CHECK:      kotlin_lpad:
; CHECK-NEXT:   %lp2 = landingpad
; CHECK-NEXT:     cleanup
; CHECK-NEXT:   br label %kotlin_lpad.body
; CHECK:      kotlin_lpad.body:
; CHECK-NEXT:   %eh = phi
; CHECK-NEXT:   call void @SetCurrentFrame(ptr %shadow_stack_frame)
define void @untagged_runtime_pad_calling_kotlin() #0 personality ptr @__gxx_personality_v0 {
entry:
  invoke void @throwing_call() to label %cont unwind label %runtime_lpad

cont:
  invoke void @throwing_call() to label %done unwind label %kotlin_lpad

runtime_lpad:
  %lp = landingpad { ptr, i32 } catch ptr @exception_type_info
  %selector = extractvalue { ptr, i32 } %lp, 1
  %matches = icmp eq i32 %selector, 1
  br i1 %matches, label %catch, label %kotlin_lpad.body

catch:
  call void @kotlin_func()
  br label %done

done:
  ret void

kotlin_lpad:
  %lp2 = landingpad { ptr, i32 } cleanup, !kotlin.gc.landingpad !0
  br label %kotlin_lpad.body

kotlin_lpad.body:
  %eh = phi { ptr, i32 } [ %lp, %runtime_lpad ], [ %lp2, %kotlin_lpad ]
  call void @Kotlin_gc_frameSetCurrent()
  resume { ptr, i32 } %eh
}

declare void @kotlin_func() #0
@exception_type_info = external constant ptr

!0 = !{}

; CHECK-LABEL: define void @untagged_pad_without_roots()
; CHECK-NOT: alloca
; CHECK-NOT: EnterFrame
; CHECK: lpad:
; CHECK-NOT: LeaveFrame
; CHECK: resume
define void @untagged_pad_without_roots() #0 personality ptr @__gxx_personality_v0 {
entry:
  invoke void @throwing_call() to label %cont unwind label %lpad
cont:
  ret void
lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @safepoint_1()
  resume { ptr, i32 } %lp
}
