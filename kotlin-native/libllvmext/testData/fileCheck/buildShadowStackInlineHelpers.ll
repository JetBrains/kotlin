; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

; CHECK-NOT: define {{.*}}@EnterFrame
; CHECK-NOT: define {{.*}}@LeaveFrame
; CHECK-NOT: define {{.*}}@SetCurrentFrame

@currentThread = external thread_local global ptr
@otherThreadLocal = external thread_local global ptr

declare void @safepoint_1()
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @produce()

define internal void @EnterFrame(ptr %frame, i32 %parameters, i32 %count) {
  %tls = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
  %thread = load ptr, ptr %tls, align 8
  %top = load ptr, ptr %thread, align 8
  store ptr %top, ptr %frame, align 8
  store ptr %frame, ptr %thread, align 8
  %countAddr = getelementptr inbounds i8, ptr %frame, i64 12
  store i32 %count, ptr %countAddr, align 4
  ret void
}

define internal void @LeaveFrame(ptr %frame, i32 %parameters, i32 %count) {
  %tls = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
  %thread = load ptr, ptr %tls, align 8
  %previous = load ptr, ptr %frame, align 8
  store ptr %previous, ptr %thread, align 8
  ret void
}

define internal void @SetCurrentFrame(ptr %frame) {
  %tls = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
  %thread = load ptr, ptr %tls, align 8
  store ptr %frame, ptr %thread, align 8
  ret void
}

; CHECK-LABEL: define void @own_lookup_same_block()
; CHECK: [[TLS:%[0-9a-z_.]+]] = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
; CHECK-NOT: call void @EnterFrame
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: load ptr, ptr [[TLS]]
; CHECK: store ptr %shadow_stack_frame, ptr
; CHECK-NOT: @llvm.threadlocal.address
; CHECK-NOT: call void @LeaveFrame
; CHECK: load ptr, ptr [[TLS]]
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: ret void
define void @own_lookup_same_block() #0 {
entry:
  %tls = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
  %thread = load ptr, ptr %tls, align 8
  %obj = call ptr addrspace(1) @produce()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

; CHECK-LABEL: define void @frame_helpers_only(ptr %arg)
; CHECK-SAME: personality
; CHECK: [[TLS:%[0-9a-z_.]+]] = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: landingpad
; CHECK-NOT: call void @SetCurrentFrame
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: load ptr, ptr [[TLS]]
; CHECK-NOT: @llvm.threadlocal.address
; CHECK-NOT: call void @LeaveFrame
; CHECK: load ptr, ptr [[TLS]]
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: resume
define void @frame_helpers_only(ptr addrspace(1) %arg) #0 personality ptr @__gxx_personality_v0 {
entry:
  invoke void @safepoint_1() to label %cont unwind label %lpad

cont:
  call void @use_func(ptr addrspace(1) %arg)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  call void @use_func(ptr addrspace(1) %arg)
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @own_lookup_later(i1 %c)
; CHECK: [[TLS:%[0-9a-z_.]+]] = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
; CHECK: store ptr %shadow_stack_frame, ptr
; CHECK: then:
; CHECK-NEXT: %thread = load ptr, ptr [[TLS]]
; CHECK-NEXT: call ptr @llvm.threadlocal.address.p0(ptr @otherThreadLocal)
; CHECK-NOT: @llvm.threadlocal.address
; CHECK: ret void
define void @own_lookup_later(i1 %c) #0 {
entry:
  %obj = call ptr addrspace(1) @produce()
  call void @safepoint_1()
  br i1 %c, label %then, label %exit

then:
  %tls = call ptr @llvm.threadlocal.address.p0(ptr @currentThread)
  %thread = load ptr, ptr %tls, align 8
  %other = call ptr @llvm.threadlocal.address.p0(ptr @otherThreadLocal)
  %otherValue = load ptr, ptr %other, align 8
  br label %exit

exit:
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

declare void @Kotlin_gc_frameSetCurrent()
declare i32 @__gxx_personality_v0(...)
declare ptr @llvm.threadlocal.address.p0(ptr)
