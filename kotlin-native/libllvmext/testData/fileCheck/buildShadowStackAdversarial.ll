; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

; CHECK-NOT: addrspace(1)
; CHECK: declare ptr @uncalled_decl_multi(ptr, i32, ptr)
; CHECK: declare ptr @uncalled_vararg(ptr, ...)
; CHECK: declare void @uncalled_void_decl(ptr)
; CHECK: declare ptr @called_multi_decl(ptr, ptr)
; CHECK-NOT: addrspace(1)

declare ptr addrspace(1) @uncalled_decl_multi(ptr addrspace(1), i32, ptr addrspace(1))
declare ptr addrspace(1) @uncalled_vararg(ptr addrspace(1), ...)
declare void @uncalled_void_decl(ptr addrspace(1))
declare ptr addrspace(1) @called_multi_decl(ptr addrspace(1), ptr addrspace(1))
declare void @safepoint_call()
declare i32 @__gxx_personality_v0(...)

; CHECK-LABEL: define ptr @test_called_decl(ptr %a, ptr %b)
; CHECK-NOT: addrspace(1)
; CHECK: %res = call ptr @called_multi_decl(ptr %a, ptr %b)
; CHECK: ret ptr %res
define ptr addrspace(1) @test_called_decl(ptr addrspace(1) %a, ptr addrspace(1) %b) #0 {
entry:
  %res = call ptr addrspace(1) @called_multi_decl(ptr addrspace(1) %a, ptr addrspace(1) %b)
  ret ptr addrspace(1) %res
}

; CHECK-LABEL: define ptr @test_phi_nulls(i32 %selector, ptr %arg)
; CHECK-NOT: addrspace(1)
; CHECK: merge:
; CHECK: %res = phi ptr [ %arg, %case0 ], [ null, %case1 ], [ null, %case2 ], [ null, %default ]
; CHECK: ret ptr %res
define ptr addrspace(1) @test_phi_nulls(i32 %selector, ptr addrspace(1) %arg) #0 {
entry:
  switch i32 %selector, label %default [
    i32 0, label %case0
    i32 1, label %case1
    i32 2, label %case2
  ]

case0:
  br label %merge

case1:
  br label %merge

case2:
  br label %merge

default:
  br label %merge

merge:
  %res = phi ptr addrspace(1) [ %arg, %case0 ], [ null, %case1 ], [ null, %case2 ], [ null, %default ]
  ret ptr addrspace(1) %res
}

; CHECK-LABEL: define i1 @test_select_icmp_nulls(i1 %c1, i1 %c2, ptr %p)
; CHECK-NOT: addrspace(1)
; CHECK: %sel1 = select i1 %c1, ptr %p, ptr null
; CHECK: %sel2 = select i1 %c2, ptr null, ptr undef
; CHECK: %sel3 = select i1 %c1, ptr poison, ptr null
; CHECK: %cmp1 = icmp eq ptr %sel1, null
; CHECK: %cmp2 = icmp ne ptr %sel2, null
; CHECK: %cmp3 = icmp eq ptr %sel3, poison
; CHECK: %cmp4 = icmp eq ptr null, null
define i1 @test_select_icmp_nulls(i1 %c1, i1 %c2, ptr addrspace(1) %p) #0 {
entry:
  %sel1 = select i1 %c1, ptr addrspace(1) %p, ptr addrspace(1) null
  %sel2 = select i1 %c2, ptr addrspace(1) null, ptr addrspace(1) undef
  %sel3 = select i1 %c1, ptr addrspace(1) poison, ptr addrspace(1) null
  %cmp1 = icmp eq ptr addrspace(1) %sel1, null
  %cmp2 = icmp ne ptr addrspace(1) %sel2, null
  %cmp3 = icmp eq ptr addrspace(1) %sel3, poison
  %cmp4 = icmp eq ptr addrspace(1) null, null
  %and1 = and i1 %cmp1, %cmp2
  %and2 = and i1 %cmp3, %cmp4
  %res = and i1 %and1, %and2
  ret i1 %res
}

; CHECK-LABEL: define ptr @test_store_call_invoke_null(ptr %slot)
; CHECK-NOT: addrspace(1)
; CHECK: store ptr null, ptr %slot
; CHECK: store ptr undef, ptr %slot
; CHECK: store ptr poison, ptr %slot
; CHECK: %c = call ptr @called_multi_decl(ptr null, ptr null)
; CHECK: %inv = invoke ptr @called_multi_decl(ptr null, ptr %c)
; CHECK:         to label %invoke_ok unwind label %lpad
; CHECK: invoke_ok:
; CHECK: ret ptr %inv
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK:   cleanup
; CHECK: ret ptr null
define ptr addrspace(1) @test_store_call_invoke_null(ptr %slot) #0 personality ptr @__gxx_personality_v0 {
entry:
  store ptr addrspace(1) null, ptr %slot
  store ptr addrspace(1) undef, ptr %slot
  store ptr addrspace(1) poison, ptr %slot
  %c = call ptr addrspace(1) @called_multi_decl(ptr addrspace(1) null, ptr addrspace(1) null)
  %inv = invoke ptr addrspace(1) @called_multi_decl(ptr addrspace(1) null, ptr addrspace(1) %c)
          to label %invoke_ok unwind label %lpad

invoke_ok:
  ret ptr addrspace(1) %inv

lpad:
  %lp = landingpad { ptr, i32 }
          cleanup
  ret ptr addrspace(1) null
}

; CHECK-LABEL: define void @test_loop_phi_null(i32 %n)
; CHECK-NOT: addrspace(1)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: loop:
; CHECK: %ref = phi ptr [ null, %entry ], [ %next_ref, %loop ]
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %ref, ptr %slot_0
; CHECK: call void @safepoint_call()
; CHECK: %ignored = call ptr @called_multi_decl(ptr %ref, ptr null)
; CHECK: %next_ref = call ptr @called_multi_decl(ptr null, ptr null)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @test_loop_phi_null(i32 %n) #0 {
entry:
  br label %loop

loop:
  %i = phi i32 [ 0, %entry ], [ %i_next, %loop ]
  %ref = phi ptr addrspace(1) [ null, %entry ], [ %next_ref, %loop ]
  call void @safepoint_call()
  %ignored = call ptr addrspace(1) @called_multi_decl(ptr addrspace(1) %ref, ptr addrspace(1) null)
  %next_ref = call ptr addrspace(1) @called_multi_decl(ptr addrspace(1) null, ptr addrspace(1) null)
  %i_next = add i32 %i, 1
  %cond = icmp slt i32 %i_next, %n
  br i1 %cond, label %loop, label %exit

exit:
  ret void
}
