; OPT: --passes=kotlin-build-shadow-stack,verify
; FILECHECK: --implicit-check-not=Kotlin_gc_frame
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @Kotlin_initRuntimeIfNeeded()
declare void @Kotlin_mm_switchThreadStateRunnable()
declare void @Kotlin_mm_switchThreadStateNative()
declare void @Kotlin_mm_safePointFunctionPrologue()
declare void @Kotlin_gc_frameEnter()
declare void @Kotlin_gc_frameLeave()
declare void @Kotlin_gc_frameSetCurrent()
declare ptr addrspace(1) @allocate()
declare ptr @Kotlin_Any_getTypeInfo(ptr addrspace(1))
declare void @safepoint_1()
declare void @use_ref(ptr addrspace(1))
declare void @throwing_call()
declare i32 @__gxx_personality_v0(...)

; CHECK-LABEL: define void @bridge_from_c(ptr %arg)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: store ptr %arg, ptr %slot_0
; CHECK: call void @Kotlin_initRuntimeIfNeeded()
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateRunnable()
; CHECK-NEXT: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_safePointFunctionPrologue()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: ret void
define void @bridge_from_c(ptr addrspace(1) %arg) #0 {
prologue:
  br label %stack_locals_init

stack_locals_init:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %entry

entry:
  call void @safepoint_1()
  call void @use_ref(ptr addrspace(1) %arg)
  br label %epilogue

epilogue:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  ret void
}

; CHECK-LABEL: define void @bridge_unwinds()
; CHECK: call void @Kotlin_mm_switchThreadStateRunnable()
; CHECK-NEXT: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 2)
; CHECK: %lp = landingpad
; CHECK-NEXT: cleanup
; CHECK-NEXT: call void @SetCurrentFrame(ptr %shadow_stack_frame)
; CHECK-NEXT: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 2)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: resume
define void @bridge_unwinds() #0 personality ptr @__gxx_personality_v0 {
prologue:
  br label %stack_locals_init

stack_locals_init:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  br label %entry

entry:
  invoke void @throwing_call() to label %epilogue unwind label %cleanup_landingpad

epilogue:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  ret void

cleanup_landingpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @handler_entered_in_native_state()
; CHECK: %lp = landingpad
; CHECK-NEXT: cleanup
; CHECK-NEXT: catch
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateRunnable()
; CHECK-NEXT: call void @SetCurrentFrame(ptr %shadow_stack_frame)
define void @handler_entered_in_native_state() #0 personality ptr @__gxx_personality_v0 {
entry:
  %obj = call ptr addrspace(1) @allocate()
  call void @Kotlin_mm_switchThreadStateNative()
  invoke void @throwing_call() to label %cont unwind label %lpad

cont:
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @use_ref(ptr addrspace(1) %obj)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } catch ptr null
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameSetCurrent()
  call void @use_ref(ptr addrspace(1) %obj)
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @bridge_argument_is_rooted(ptr %arg)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: store ptr %arg, ptr %slot_0
define void @bridge_argument_is_rooted(ptr addrspace(1) %arg) #0 {
prologue:
  br label %stack_locals_init

stack_locals_init:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %entry

entry:
  %ti = call ptr @Kotlin_Any_getTypeInfo(ptr addrspace(1) %arg)
  br label %epilogue

epilogue:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  ret void
}

; CHECK-LABEL: define void @kotlin_argument_is_caller_rooted(ptr %arg)
; CHECK-NOT: alloca
; CHECK-NOT: @EnterFrame
; CHECK: call void @safepoint_1()
; CHECK: ret void
define void @kotlin_argument_is_caller_rooted(ptr addrspace(1) %arg) #0 {
prologue:
  br label %stack_locals_init

stack_locals_init:
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %entry

entry:
  call void @safepoint_1()
  %ti = call ptr @Kotlin_Any_getTypeInfo(ptr addrspace(1) %arg)
  ret void
}

; CHECK-LABEL: define void @bridge_without_frame()
; CHECK-NOT: alloca
; CHECK-NOT: @EnterFrame
; CHECK: ret void
define void @bridge_without_frame() #0 {
prologue:
  br label %stack_locals_init

stack_locals_init:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %entry

entry:
  call void @safepoint_1()
  br label %epilogue

epilogue:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  ret void
}

; CHECK-LABEL: define i32 @bridge_with_merged_prologue(ptr %arg)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @llvm.memset.p0.i64(ptr align 8 %shadow_stack_frame, i8 0, i64 24, i1 false)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %arg, ptr %slot_0, align 8
; CHECK: call void @Kotlin_initRuntimeIfNeeded()
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateRunnable()
; CHECK-NEXT: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @safepoint_1()
; CHECK: %v = load i32, ptr %arg
; CHECK-NEXT: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: ret i32 %v
define i32 @bridge_with_merged_prologue(ptr addrspace(1) %arg) #0 {
entry:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @safepoint_1()
  %v = load i32, ptr addrspace(1) %arg
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  ret i32 %v
}

; CHECK-LABEL: define i32 @bridge_with_merged_returns(ptr %arg, i1 %c)
; CHECK: call void @Kotlin_mm_switchThreadStateRunnable()
; CHECK-NEXT: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ok:
; CHECK-NEXT: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: br label %common.ret
; CHECK: common.ret:
; CHECK-NEXT: %r = phi i32
; CHECK-NEXT: ret i32 %r
; CHECK: failed:
; CHECK-NEXT: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: br label %common.ret
define i32 @bridge_with_merged_returns(ptr addrspace(1) %arg, i1 %c) #0 {
entry:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @safepoint_1()
  call void @use_ref(ptr addrspace(1) %arg)
  br i1 %c, label %ok, label %failed

ok:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  br label %common.ret

common.ret:
  %r = phi i32 [ 1, %ok ], [ 0, %failed ]
  ret i32 %r

failed:
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  br label %common.ret
}

; CHECK-LABEL: define ptr @bridge_returns_autoreleased(ptr %arg)
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: {{^}}  call void @use_ref(ptr %arg)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: call void @Kotlin_mm_switchThreadStateNative()
; CHECK-NEXT: %r = tail call ptr @llvm.objc.autoreleaseReturnValue(ptr %obj)
; CHECK-NEXT: ret ptr %r
define ptr @bridge_returns_autoreleased(ptr addrspace(1) %arg) #0 {
entry:
  call void @Kotlin_initRuntimeIfNeeded()
  call void @Kotlin_mm_switchThreadStateRunnable()
  call void @Kotlin_gc_frameEnter()
  call void @safepoint_1()
  tail call void @use_ref(ptr addrspace(1) %arg)
  %obj = call ptr @Kotlin_Any_getTypeInfo(ptr addrspace(1) %arg)
  call void @Kotlin_gc_frameLeave()
  call void @Kotlin_mm_switchThreadStateNative()
  %r = tail call ptr @llvm.objc.autoreleaseReturnValue(ptr %obj)
  ret ptr %r
}

declare ptr @llvm.objc.autoreleaseReturnValue(ptr)
