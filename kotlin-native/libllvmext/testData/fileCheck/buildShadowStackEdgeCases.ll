; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @safepoint()
declare void @safepoint_1()
declare void @safepoint_2()
declare void @safepoint_3()
declare void @safepoint_4()
declare ptr addrspace(1) @alloc_func()
declare void @use_func(ptr addrspace(1))
declare void @pure_use(ptr addrspace(1)) memory(read) nounwind
declare void @pure_use_both(ptr addrspace(1), ptr addrspace(1)) memory(read) nounwind
declare void @safepoint_outer()
declare void @safepoint_inner()
declare i32 @__gxx_personality_v0(...)
declare void @Kotlin_gc_frameSetCurrent()
declare void @Kotlin_initRuntimeIfNeeded()
declare void @Kotlin_mm_switchThreadStateRunnable()
declare void @Kotlin_mm_switchThreadStateNative()
declare void @Kotlin_gc_frameEnter()
declare void @Kotlin_gc_frameLeave()

; CHECK-LABEL: define i32 @pure_leaf(ptr %arg)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: ret i32 %v
define i32 @pure_leaf(ptr addrspace(1) %arg) #0 {
entry:
  %v = load i32, ptr addrspace(1) %arg
  ret i32 %v
}

; CHECK-LABEL: define i32 @mixed_straight()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %live, ptr %slot_0, align 8
; CHECK-NOT: store ptr %dead
; CHECK: call void @safepoint()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret i32 %sum
define i32 @mixed_straight() #0 {
entry:
  %live = call ptr addrspace(1) @alloc_func()
  %dead = call ptr addrspace(1) @alloc_func()
  %d = load i32, ptr addrspace(1) %dead
  call void @safepoint()
  %l = load i32, ptr addrspace(1) %live
  %sum = add i32 %d, %l
  ret i32 %sum
}

; CHECK-LABEL: define void @unreachable_safepoint()
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: ret void
define void @unreachable_safepoint() #0 {
entry:
  ret void

unreachable_bb:
  %obj = call ptr addrspace(1) @alloc_func()
  call void @safepoint()
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

; CHECK-LABEL: define void @unreachable_def(ptr %field)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK: call void @safepoint()
; CHECK-NOT: LeaveFrame
; CHECK: ret void
define void @unreachable_def(ptr %field) #0 {
entry:
  call void @safepoint()
  ret void

dead_bb:
  %dead_root = load ptr addrspace(1), ptr %field
  call void @use_func(ptr addrspace(1) %dead_root)
  ret void
}

; CHECK-LABEL: define void @many_simultaneous_roots()
; CHECK: %shadow_stack_frame = alloca [10 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 10)
; CHECK: call void @safepoint()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 10)
; CHECK: ret void
define void @many_simultaneous_roots() #0 {
entry:
  %r0 = call ptr addrspace(1) @alloc_func()
  %r1 = call ptr addrspace(1) @alloc_func()
  %r2 = call ptr addrspace(1) @alloc_func()
  %r3 = call ptr addrspace(1) @alloc_func()
  %r4 = call ptr addrspace(1) @alloc_func()
  %r5 = call ptr addrspace(1) @alloc_func()
  %r6 = call ptr addrspace(1) @alloc_func()
  %r7 = call ptr addrspace(1) @alloc_func()
  call void @safepoint()
  call void @use_func(ptr addrspace(1) %r0)
  call void @use_func(ptr addrspace(1) %r1)
  call void @use_func(ptr addrspace(1) %r2)
  call void @use_func(ptr addrspace(1) %r3)
  call void @use_func(ptr addrspace(1) %r4)
  call void @use_func(ptr addrspace(1) %r5)
  call void @use_func(ptr addrspace(1) %r6)
  call void @use_func(ptr addrspace(1) %r7)
  ret void
}

; CHECK-LABEL: define i32 @multi_block_loop_dead_before_safepoint(ptr %field)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: bb_safe:
; CHECK: call void @safepoint()
; CHECK: br label %bb_def
define i32 @multi_block_loop_dead_before_safepoint(ptr %field) #0 {
entry:
  br label %bb_def

bb_def:
  %v = load ptr addrspace(1), ptr %field
  br label %bb_mid

bb_mid:
  %val = load i32, ptr addrspace(1) %v
  br label %bb_safe

bb_safe:
  call void @safepoint()
  br label %bb_def
}

; CHECK-LABEL: define i32 @loop_used_in_latch_before_safepoint(ptr %field)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: bb_latch:
; CHECK: call void @safepoint()
; CHECK: br label %bb_header
define i32 @loop_used_in_latch_before_safepoint(ptr %field) #0 {
entry:
  br label %bb_header

bb_header:
  %v = load ptr addrspace(1), ptr %field
  br label %bb_latch

bb_latch:
  %val = load i32, ptr addrspace(1) %v
  call void @safepoint()
  br label %bb_header
}

; CHECK-LABEL: define void @nested_loop_outer_used_in_inner(ptr %field, i1 %inner_cond, i1 %outer_cond)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %outer_root, ptr %slot_0
; CHECK: inner_header:
; CHECK: call void @safepoint()
; CHECK: call void @pure_use(ptr %outer_root)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @nested_loop_outer_used_in_inner(ptr %field, i1 %inner_cond, i1 %outer_cond) #0 {
entry:
  br label %outer_header

outer_header:
  %outer_root = load ptr addrspace(1), ptr %field
  br label %inner_header

inner_header:
  call void @safepoint()
  call void @pure_use(ptr addrspace(1) %outer_root)
  br i1 %inner_cond, label %inner_header, label %outer_latch

outer_latch:
  br i1 %outer_cond, label %outer_header, label %exit

exit:
  ret void
}

; CHECK-LABEL: define i32 @nested_loop_outer_dead_before_inner(ptr %field, i1 %inner_cond, i1 %outer_cond)
; CHECK-NOT: alloca [
; CHECK-NOT: EnterFrame
; CHECK-NOT: LeaveFrame
; CHECK: inner_header:
; CHECK: call void @safepoint()
; CHECK: ret i32 %val
define i32 @nested_loop_outer_dead_before_inner(ptr %field, i1 %inner_cond, i1 %outer_cond) #0 {
entry:
  br label %outer_header

outer_header:
  %outer_root = load ptr addrspace(1), ptr %field
  %val = load i32, ptr addrspace(1) %outer_root
  br label %inner_header

inner_header:
  call void @safepoint()
  br i1 %inner_cond, label %inner_header, label %outer_latch

outer_latch:
  br i1 %outer_cond, label %outer_header, label %exit

exit:
  ret i32 %val
}

; CHECK-LABEL: define void @loop_multiple_safepoints_slot_sharing(i1 %cond)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %v1, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: %slot_01 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %v2, ptr %slot_01
; CHECK: call void @safepoint_2()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @loop_multiple_safepoints_slot_sharing(i1 %cond) #0 {
entry:
  br label %loop

loop:
  %v1 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %v1)

  %v2 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %v2)

  br i1 %cond, label %loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @loop_multiple_safepoints_overlapping(i1 %cond)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %v1, ptr %slot_0
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: store ptr %v2, ptr %slot_1
; CHECK: call void @safepoint_1()
; CHECK: call void @safepoint_2()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: ret void
define void @loop_multiple_safepoints_overlapping(i1 %cond) #0 {
entry:
  br label %loop

loop:
  %v1 = call ptr addrspace(1) @alloc_func()
  %v2 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %v1)
  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %v2)
  br i1 %cond, label %loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @loop_carried_separate_safepoints(i1 %cond)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: loop_header:
; CHECK: call void @safepoint_1()
; CHECK: call void @pure_use(ptr %cur)
; CHECK: loop_latch:
; CHECK: call void @safepoint_2()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @loop_carried_separate_safepoints(i1 %cond) #0 {
entry:
  %init = call ptr addrspace(1) @alloc_func()
  br label %loop_header

loop_header:
  %cur = phi ptr addrspace(1) [ %init, %entry ], [ %next, %loop_latch ]
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %cur)
  br label %loop_latch

loop_latch:
  %next = call ptr addrspace(1) @alloc_func()
  call void @safepoint_2()
  br i1 %cond, label %loop_header, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @loop_carried_simultaneously_live(i1 %cond)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: loop:
; CHECK: call void @safepoint_1()
; CHECK: call void @pure_use_both(ptr %cur, ptr %next)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: ret void
define void @loop_carried_simultaneously_live(i1 %cond) #0 {
entry:
  %init = call ptr addrspace(1) @alloc_func()
  br label %loop

loop:
  %cur = phi ptr addrspace(1) [ %init, %entry ], [ %next, %loop ]
  %next = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @pure_use_both(ptr addrspace(1) %cur, ptr addrspace(1) %next)
  br i1 %cond, label %loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @multi_block_loop_live_across_latch_safepoint(ptr %field, i1 %cond)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: bb_header:
; CHECK: bb_latch:
; CHECK: call void @safepoint()
; CHECK: call void @pure_use(ptr %v)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @multi_block_loop_live_across_latch_safepoint(ptr %field, i1 %cond) #0 {
entry:
  br label %bb_header

bb_header:
  %v = load ptr addrspace(1), ptr %field
  br label %bb_latch

bb_latch:
  call void @safepoint()
  call void @pure_use(ptr addrspace(1) %v)
  br i1 %cond, label %bb_header, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @deeply_nested_loops(ptr %field, i1 %c1, i1 %c2, i1 %c3)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %outer_root, ptr %slot_0
; CHECK: l3_header:
; CHECK: call void @safepoint()
; CHECK: call void @pure_use(ptr %outer_root)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @deeply_nested_loops(ptr %field, i1 %c1, i1 %c2, i1 %c3) #0 {
entry:
  br label %l1_header

l1_header:
  %outer_root = load ptr addrspace(1), ptr %field
  br label %l2_header

l2_header:
  %mid_dead = load ptr addrspace(1), ptr %field
  %v = load i32, ptr addrspace(1) %mid_dead
  br label %l3_header

l3_header:
  call void @safepoint()
  call void @pure_use(ptr addrspace(1) %outer_root)
  br i1 %c3, label %l3_header, label %l2_latch

l2_latch:
  br i1 %c2, label %l2_header, label %l1_latch

l1_latch:
  br i1 %c1, label %l1_header, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @diamond_branch_safepoint(i1 %cond, ptr %field)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: bb_then:
; CHECK: call void @safepoint()
; CHECK: call void @pure_use(ptr %root)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @diamond_branch_safepoint(i1 %cond, ptr %field) #0 {
entry:
  %root = load ptr addrspace(1), ptr %field
  br i1 %cond, label %bb_then, label %bb_else

bb_then:
  call void @safepoint()
  call void @pure_use(ptr addrspace(1) %root)
  br label %merge

bb_else:
  br label %merge

merge:
  ret void
}

; CHECK-LABEL: define void @triangle_interference_k3()
; CHECK: %shadow_stack_frame = alloca [5 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 5)
; CHECK: call void @safepoint_1()
; CHECK: call void @safepoint_2()
; CHECK: call void @safepoint_3()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 5)
; CHECK: ret void
define void @triangle_interference_k3() #0 {
entry:
  %r1 = call ptr addrspace(1) @alloc_func()
  %r2 = call ptr addrspace(1) @alloc_func()
  %r3 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %r1)
  call void @pure_use(ptr addrspace(1) %r2)

  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %r2)
  call void @pure_use(ptr addrspace(1) %r3)

  call void @safepoint_3()
  call void @pure_use(ptr addrspace(1) %r3)
  call void @pure_use(ptr addrspace(1) %r1)

  ret void
}

; CHECK-LABEL: define void @bipartite_interference_c4(i1 %branch, ptr %ra, ptr %rb, ptr %rc, ptr %rd)
; CHECK: %shadow_stack_frame = alloca [6 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 6)
; CHECK: store ptr %ra, ptr %slot_0
; CHECK: store ptr %rb, ptr %slot_1
; CHECK: store ptr %rc, ptr %slot_2
; CHECK: store ptr %rd, ptr %slot_3
; CHECK: left:
; CHECK: call void @safepoint_1()
; CHECK: call void @safepoint_2()
; CHECK: right:
; CHECK: call void @safepoint_3()
; CHECK: call void @safepoint_4()
; CHECK: exit:
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 6)
; CHECK: ret void
define void @bipartite_interference_c4(
    i1 %branch, ptr addrspace(1) %ra, ptr addrspace(1) %rb, ptr addrspace(1) %rc, ptr addrspace(1) %rd) #0 {
entry:
  call void @Kotlin_gc_frameEnter()
  br i1 %branch, label %left, label %right

left:
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %ra)
  call void @pure_use(ptr addrspace(1) %rc)

  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %ra)
  call void @pure_use(ptr addrspace(1) %rd)
  br label %exit

right:
  call void @safepoint_3()
  call void @pure_use(ptr addrspace(1) %rb)
  call void @pure_use(ptr addrspace(1) %rc)

  call void @safepoint_4()
  call void @pure_use(ptr addrspace(1) %rb)
  call void @pure_use(ptr addrspace(1) %rd)
  br label %exit

exit:
  call void @Kotlin_gc_frameLeave()
  ret void
}

; CHECK-LABEL: define void @test_invoke_root_live()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @llvm.memset{{.*}}%shadow_stack_frame
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %r = invoke ptr @alloc_func()
; CHECK:         to label %normal unwind label %lpad
; CHECK: normal:
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %r, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %r)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK:   cleanup
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: resume { ptr, i32 } %lp
define void @test_invoke_root_live() #0 personality ptr @__gxx_personality_v0 {
entry:
  %r = invoke ptr addrspace(1) @alloc_func()
         to label %normal unwind label %lpad

normal:
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %r)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @test_catch_landingpad_cleanup_injection()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK-NEXT: cleanup
; CHECK-NEXT: catch ptr null
; CHECK: call void @SetCurrentFrame(ptr %shadow_stack_frame)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @test_catch_landingpad_cleanup_injection() #0 personality ptr @__gxx_personality_v0 {
entry:
  %r = invoke ptr addrspace(1) @alloc_func()
         to label %normal unwind label %lpad

normal:
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %r)
  ret void

lpad:
  %lp = landingpad { ptr, i32 }
          catch ptr null
  call void @Kotlin_gc_frameSetCurrent()
  call void @safepoint_1()
  ret void
}

; CHECK-LABEL: define void @test_multi_invoke_shared_lpad()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: store ptr %obj, ptr %slot_0
; CHECK: invoke void @safepoint_1()
; CHECK:         to label %next unwind label %lpad
; CHECK: next:
; CHECK: invoke void @safepoint_1()
; CHECK:         to label %normal unwind label %lpad
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK:   cleanup
; CHECK: br label %cleanup_bb
; CHECK: cleanup_bb:
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: resume { ptr, i32 } %lp
define void @test_multi_invoke_shared_lpad() #0 personality ptr @__gxx_personality_v0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  invoke void @safepoint_1()
    to label %next unwind label %lpad

next:
  invoke void @safepoint_1()
    to label %normal unwind label %lpad

normal:
  call void @use_func(ptr addrspace(1) %obj)
  ret void

lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  br label %cleanup_bb

cleanup_bb:
  resume { ptr, i32 } %lp
}

; CHECK-LABEL: define void @test_c4_bipartite()
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: store ptr %r1, ptr %slot_0
; CHECK: store ptr %r4, ptr %slot_1
; CHECK: call void @safepoint_1()
; CHECK: store ptr %r2, ptr %slot_11
; CHECK: call void @safepoint_2()
; CHECK: store ptr %r3, ptr %slot_02
; CHECK: call void @safepoint_3()
; CHECK: store ptr %r4_new, ptr %slot_13
; CHECK: call void @safepoint_4()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: ret void
define void @test_c4_bipartite() #0 {
entry:
  %r1 = call ptr addrspace(1) @alloc_func()
  %r4 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %r1)
  call void @pure_use(ptr addrspace(1) %r4)

  %r2 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %r1)
  call void @pure_use(ptr addrspace(1) %r2)

  %r3 = call ptr addrspace(1) @alloc_func()
  call void @safepoint_3()
  call void @pure_use(ptr addrspace(1) %r2)
  call void @pure_use(ptr addrspace(1) %r3)

  %r4_new = call ptr addrspace(1) @alloc_func()
  call void @safepoint_4()
  call void @pure_use(ptr addrspace(1) %r3)
  call void @pure_use(ptr addrspace(1) %r4_new)

  ret void
}

; CHECK-LABEL: define void @test_nested_loop_carried(i1 %c_outer, i1 %c_inner)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: store ptr %acc_out, ptr %slot_0
; CHECK: call void @safepoint_outer()
; CHECK: store ptr %acc_in, ptr %slot_1
; CHECK: call void @safepoint_inner()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: ret void
define void @test_nested_loop_carried(i1 %c_outer, i1 %c_inner) #0 {
entry:
  %init_out = call ptr addrspace(1) @alloc_func()
  br label %outer_loop

outer_loop:
  %acc_out = phi ptr addrspace(1) [ %init_out, %entry ], [ %next_out, %outer_latch ]
  call void @safepoint_outer()
  call void @pure_use(ptr addrspace(1) %acc_out)
  %init_in = call ptr addrspace(1) @alloc_func()
  br label %inner_loop

inner_loop:
  %acc_in = phi ptr addrspace(1) [ %init_in, %outer_loop ], [ %next_in, %inner_loop ]
  call void @safepoint_inner()
  call void @pure_use_both(ptr addrspace(1) %acc_out, ptr addrspace(1) %acc_in)
  %next_in = call ptr addrspace(1) @alloc_func()
  br i1 %c_inner, label %inner_loop, label %outer_latch

outer_latch:
  %next_out = call ptr addrspace(1) @alloc_func()
  br i1 %c_outer, label %outer_loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @test_arguments_divergent_branches(i1 %cond, ptr %argA, ptr %argB)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %argA, ptr %slot_0, align 8
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: store ptr %argB, ptr %slot_1, align 8
; CHECK: then:
; CHECK: call void @safepoint_1()
; CHECK: call void @pure_use(ptr %argA)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: else:
; CHECK: call void @safepoint_2()
; CHECK: call void @pure_use(ptr %argB)
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK: ret void
define void @test_arguments_divergent_branches(i1 %cond, ptr addrspace(1) %argA, ptr addrspace(1) %argB) #0 {
entry:
  call void @Kotlin_gc_frameEnter()
  br i1 %cond, label %then, label %else

then:
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %argA)
  call void @Kotlin_gc_frameLeave()
  ret void

else:
  call void @safepoint_2()
  call void @pure_use(ptr addrspace(1) %argB)
  call void @Kotlin_gc_frameLeave()
  ret void
}

; CHECK-LABEL: define void @test_catch_landingpad_resync()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: lpad:
; CHECK: %lp = landingpad { ptr, i32 }
; CHECK: call void @SetCurrentFrame(ptr %shadow_stack_frame)
; CHECK: catch_handler:
; CHECK: call void @safepoint_1()
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret void
define void @test_catch_landingpad_resync() #0 personality ptr @__gxx_personality_v0 {
entry:
  %r = invoke ptr addrspace(1) @alloc_func()
         to label %normal unwind label %lpad

normal:
  call void @safepoint_1()
  call void @pure_use(ptr addrspace(1) %r)
  ret void

lpad:
  %lp = landingpad { ptr, i32 }
          catch ptr null
  call void @Kotlin_gc_frameSetCurrent()
  br label %catch_handler

catch_handler:
  call void @safepoint_1()
  ret void
}

; CHECK-LABEL: define i32 @test_gep_interior_pointer_not_rooted()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: %base = call ptr @alloc_func()
; CHECK: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK: store ptr %base, ptr %slot_0, align 8
; CHECK-NOT: %slot_1
; CHECK: %gep = getelementptr inbounds i32, ptr %base, i64 2
; CHECK-NOT: store ptr %gep
; CHECK: call void @safepoint_1()
; CHECK: %v = load i32, ptr %gep
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK: ret i32 %v
define i32 @test_gep_interior_pointer_not_rooted() #0 {
entry:
  %base = call ptr addrspace(1) @alloc_func()
  %gep = getelementptr inbounds i32, ptr addrspace(1) %base, i64 2
  call void @safepoint_1()
  %v = load i32, ptr addrspace(1) %gep
  ret i32 %v
}

; CHECK-LABEL: define void @test_keep_alive_marker()
; CHECK: %o = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %o, ptr %slot_0, align 8
; CHECK: call void @raw_call(ptr %raw)
; CHECK-NOT: Kotlin_gc_keepAlive
; CHECK: call void @LeaveFrame(ptr %shadow_stack_frame, i32 0, i32 3)
; CHECK-NEXT: ret void
declare void @raw_call(ptr)
declare void @Kotlin_gc_keepAlive(ptr addrspace(1))

define void @test_keep_alive_marker() #0 {
entry:
  %o = call ptr addrspace(1) @alloc_func()
  %f = getelementptr inbounds i8, ptr addrspace(1) %o, i64 8
  %raw = load ptr, ptr addrspace(1) %f
  call void @raw_call(ptr %raw)
  call void @Kotlin_gc_keepAlive(ptr addrspace(1) %o)
  ret void
}

; CHECK-LABEL: define void @test_no_keep_alive_marker()
; CHECK-NOT: alloca [
; CHECK: ret void
define void @test_no_keep_alive_marker() #0 {
entry:
  %o = call ptr addrspace(1) @alloc_func()
  %f = getelementptr inbounds i8, ptr addrspace(1) %o, i64 8
  %raw = load ptr, ptr addrspace(1) %f
  call void @raw_call(ptr %raw)
  ret void
}

; CHECK-NOT: declare void @Kotlin_gc_keepAlive
