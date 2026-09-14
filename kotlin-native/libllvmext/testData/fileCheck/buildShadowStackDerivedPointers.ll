; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @safepoint_1()
declare ptr addrspace(1) @alloc_func()
declare void @use_func(ptr addrspace(1))
declare void @raw_call(ptr)
declare ptr @raw_source()
declare void @use_i64(i64)
declare ptr @Kotlin_gc_returnSlot()
declare ptr addrspace(1) @getter_no_args(ptr)
declare ptr @Kotlin_arrayGetElementAddress(ptr addrspace(1), i32)
declare void @UpdateVolatileHeapRef(ptr, ptr addrspace(1))
declare void @llvm.memcpy.p1.p0.i64(ptr addrspace(1), ptr, i64, i1)
declare void @Kotlin_gc_frameEnter()
declare void @Kotlin_gc_frameLeave()

; CHECK-LABEL: define i32 @cast_keeps_base_live()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: %obj = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: load i32, ptr %f
; CHECK-NOT: addrspacecast
define i32 @cast_keeps_base_live() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  %f = getelementptr inbounds i8, ptr %raw, i64 16
  call void @safepoint_1()
  %v = load i32, ptr %f
  ret i32 %v
}

; CHECK-LABEL: define i32 @cast_dead_before_safepoint()
; CHECK-NOT: EnterFrame
; CHECK: ret i32
define i32 @cast_dead_before_safepoint() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  %v = load i32, ptr %raw
  call void @safepoint_1()
  ret i32 %v
}

; CHECK-LABEL: define void @cast_in_loop_phi(i32 %n)
; CHECK: %obj = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: loop:
; CHECK: call void @safepoint_1()
define void @cast_in_loop_phi(i32 %n) #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  br label %loop

loop:
  %p = phi ptr [ %raw, %entry ], [ %p.next, %loop ]
  %i = phi i32 [ 0, %entry ], [ %i.next, %loop ]
  call void @safepoint_1()
  store i32 0, ptr %p
  %p.next = getelementptr inbounds i32, ptr %p, i64 1
  %i.next = add i32 %i, 1
  %c = icmp slt i32 %i.next, %n
  br i1 %c, label %loop, label %exit

exit:
  ret void
}

; CHECK-LABEL: define void @cast_passed_to_safepoint()
; CHECK: %obj = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %obj, ptr %slot_0
; CHECK: call void @raw_call(ptr %obj)
define void @cast_passed_to_safepoint() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  call void @raw_call(ptr %raw)
  ret void
}

; CHECK-LABEL: define void @cast_round_trip()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: store ptr %obj, ptr %slot_0
; CHECK-NOT: addrspacecast
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %obj)
define void @cast_round_trip() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  %back = addrspacecast ptr %raw to ptr addrspace(1)
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %back)
  ret void
}

; CHECK-LABEL: define void @interior_cast_back()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: store ptr %obj, ptr %slot_0
; CHECK-NOT: store ptr %gep
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %gep)
define void @interior_cast_back() #0 {
entry:
  %obj = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %obj to ptr
  %gep = getelementptr inbounds i8, ptr %raw, i64 16
  %field = addrspacecast ptr %gep to ptr addrspace(1)
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %field)
  ret void
}

; CHECK-LABEL: define void @plain_pointer_used_after_cast()
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: %raw = call ptr @raw_source()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %raw, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK-NEXT: call void @raw_call(ptr %raw)
define void @plain_pointer_used_after_cast() #0 {
entry:
  %raw = call ptr @raw_source()
  %obj = addrspacecast ptr %raw to ptr addrspace(1)
  %field = getelementptr inbounds i8, ptr addrspace(1) %obj, i64 8
  store i32 1, ptr addrspace(1) %field
  call void @safepoint_1()
  call void @raw_call(ptr %raw)
  ret void
}

; CHECK-LABEL: define void @plain_pointer_use_not_dominated(i1 %c)
; CHECK-NOT: EnterFrame
; CHECK: ret void
define void @plain_pointer_use_not_dominated(i1 %c) #0 {
entry:
  %raw = call ptr @raw_source()
  br i1 %c, label %cast, label %other

cast:
  %obj = addrspacecast ptr %raw to ptr addrspace(1)
  store i32 1, ptr addrspace(1) %obj
  ret void

other:
  call void @safepoint_1()
  call void @raw_call(ptr %raw)
  ret void
}

; CHECK-LABEL: define void @element_address_keeps_array_live()
; CHECK: %arr = call ptr @alloc_func()
; CHECK-NEXT: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK-NEXT: store ptr %arr, ptr %slot_1
; CHECK: %addr = call ptr @Kotlin_arrayGetElementAddress(ptr %arr, i32 0)
; CHECK: call void @UpdateVolatileHeapRef(ptr %addr, ptr %value)
define void @element_address_keeps_array_live() #0 {
entry:
  %value = call ptr addrspace(1) @alloc_func()
  %arr = call ptr addrspace(1) @alloc_func()
  %addr = call ptr @Kotlin_arrayGetElementAddress(ptr addrspace(1) %arr, i32 0)
  call void @UpdateVolatileHeapRef(ptr %addr, ptr addrspace(1) %value)
  ret void
}

; CHECK-LABEL: define void @inttoptr_is_a_root(ptr %field)
; CHECK: %ref = inttoptr i64 %bits to ptr
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %ref, ptr %slot_0
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %ref)
define void @inttoptr_is_a_root(ptr %field) #0 {
entry:
  %bits = load i64, ptr %field
  %ref = inttoptr i64 %bits to ptr addrspace(1)
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %ref)
  ret void
}

; CHECK-LABEL: define ptr @folded_constants(i1 %c, ptr %a, ptr %out)
; CHECK: %p.base = phi ptr [ %a, %entry ], [ null, %t ]
; CHECK-NEXT: %p = phi ptr [ %a, %entry ], [ inttoptr (i64 16 to ptr), %t ]
; CHECK: store ptr %p.base, ptr %slot_0
; CHECK-NOT: store ptr %p,
; CHECK: call void @use_func(ptr inttoptr (i64 8 to ptr))
; CHECK: store ptr inttoptr (i64 24 to ptr), ptr %out
; CHECK: icmp eq ptr %p, inttoptr (i64 16 to ptr)
; CHECK: call void @use_func(ptr getelementptr (i8, ptr null, i64 16))
; CHECK: call void @use_i64(i64 ptrtoint (ptr getelementptr (i8, ptr null, i64 16) to i64))
; CHECK: load i32, ptr inttoptr (i64 40 to ptr)
; CHECK: ret ptr inttoptr (i64 32 to ptr)
; CHECK-NOT: addrspace(1)
define ptr addrspace(1) @folded_constants(i1 %c, ptr addrspace(1) %a, ptr %out) #0 {
entry:
  br i1 %c, label %t, label %m

t:
  br label %m

m:
  %p = phi ptr addrspace(1) [ %a, %entry ], [ inttoptr (i64 16 to ptr addrspace(1)), %t ]
  call void @use_func(ptr addrspace(1) inttoptr (i64 8 to ptr addrspace(1)))
  store ptr addrspace(1) inttoptr (i64 24 to ptr addrspace(1)), ptr %out
  %cmp = icmp eq ptr addrspace(1) %p, inttoptr (i64 16 to ptr addrspace(1))
  call void @use_func(ptr addrspace(1) getelementptr (i8, ptr addrspace(1) null, i64 16))
  call void @use_i64(i64 ptrtoint (ptr addrspace(1) getelementptr (i8, ptr addrspace(1) null, i64 16) to i64))
  %raw = addrspacecast ptr addrspace(1) inttoptr (i64 40 to ptr addrspace(1)) to ptr
  %v = load i32, ptr %raw
  ret ptr addrspace(1) inttoptr (i64 32 to ptr addrspace(1))
}

; CHECK-LABEL: define void @foreign_address_spaces(ptr %p, ptr %obj, ptr addrspace(272) %q)
; CHECK: %to272 = addrspacecast ptr %p to ptr addrspace(272)
; CHECK: %obj272 = addrspacecast ptr %obj to ptr addrspace(272)
; CHECK: %from272 = addrspacecast ptr addrspace(272) %q to ptr
; CHECK: store ptr %from272, ptr %slot_0
; CHECK: store i32 0, ptr addrspace(272) %to272
; CHECK: store i32 0, ptr addrspace(272) %obj272
; CHECK: call void @use_func(ptr %from272)
define void @foreign_address_spaces(ptr %p, ptr addrspace(1) %obj, ptr addrspace(272) %q) #0 {
entry:
  %to272 = addrspacecast ptr %p to ptr addrspace(272)
  %obj272 = addrspacecast ptr addrspace(1) %obj to ptr addrspace(272)
  %from272 = addrspacecast ptr addrspace(272) %q to ptr addrspace(1)
  store i32 0, ptr addrspace(272) %to272
  store i32 0, ptr addrspace(272) %obj272
  call void @use_func(ptr addrspace(1) %from272)
  ret void
}

; CHECK-LABEL: define void @aggregates_and_vectors(ptr %field, ptr %old, ptr %new, ptr %src, ptr %dst)
; CHECK: %pair = cmpxchg ptr %field, ptr %old, ptr %new seq_cst seq_cst
; CHECK: %prev = extractvalue { ptr, i1 } %pair, 0
; CHECK: store ptr %prev, ptr %slot_0
; CHECK: %vec = load <2 x ptr>, ptr %src
; CHECK: store <2 x ptr> %vec, ptr %dst
; CHECK: call void @safepoint_1()
; CHECK: call void @use_func(ptr %prev)
; CHECK-NOT: addrspace(1)
define void @aggregates_and_vectors(ptr %field, ptr addrspace(1) %old, ptr addrspace(1) %new, ptr %src, ptr %dst) #0 {
entry:
  %pair = cmpxchg ptr %field, ptr addrspace(1) %old, ptr addrspace(1) %new seq_cst seq_cst
  %prev = extractvalue { ptr addrspace(1), i1 } %pair, 0
  %vec = load <2 x ptr addrspace(1)>, ptr %src
  store <2 x ptr addrspace(1)> %vec, ptr %dst
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %prev)
  ret void
}

; CHECK-LABEL: define void @remangled_intrinsic(ptr %dst, ptr %src)
; CHECK: call void @llvm.memcpy.p0.p0.i64(ptr %dst, ptr %src, i64 8, i1 false)
; CHECK-NOT: llvm.memcpy.p1
define void @remangled_intrinsic(ptr addrspace(1) %dst, ptr %src) #0 {
entry:
  call void @llvm.memcpy.p1.p0.i64(ptr addrspace(1) %dst, ptr %src, i64 8, i1 false)
  ret void
}

; CHECK-LABEL: define void @marker_first_with_arg_root(ptr %arg)
; CHECK: %shadow_stack_frame = alloca [4 x ptr], align 8
; CHECK: %slot_1 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 3
; CHECK: %slot_0 = getelementptr inbounds [4 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %arg, ptr %slot_0
; CHECK-NEXT: call void @EnterFrame(ptr %shadow_stack_frame, i32 0, i32 4)
; CHECK-NOT: Kotlin_gc_returnSlot
; CHECK: %res = call ptr @getter_no_args(ptr %slot_1)
; CHECK-NOT: Kotlin_gc_returnSlot
; CHECK: %res2 = call ptr @getter_no_args(ptr %slot_1)
define void @marker_first_with_arg_root(ptr addrspace(1) %arg) #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %slot2 = call ptr @Kotlin_gc_returnSlot()
  call void @Kotlin_gc_frameEnter()
  %res = call ptr addrspace(1) @getter_no_args(ptr %slot)
  %res2 = call ptr addrspace(1) @getter_no_args(ptr %slot2)
  call void @use_func(ptr addrspace(1) %arg)
  call void @Kotlin_gc_frameLeave()
  ret void
}

%"kclassbody:Box" = type <{ ptr, ptr addrspace(1) }>

; CHECK-LABEL: define ptr @class_body_layout(ptr %obj, ptr %value)
; CHECK: %local = alloca %"kclassbody:Box"
; CHECK: %f = getelementptr inbounds %"kclassbody:Box", ptr %obj, i32 0, i32 1
; CHECK: %v = load ptr, ptr %f
; CHECK: %lf = getelementptr inbounds %"kclassbody:Box", ptr %local, i32 0, i32 1
; CHECK: store ptr %value, ptr %lf
define ptr addrspace(1) @class_body_layout(ptr addrspace(1) %obj, ptr addrspace(1) %value) #0 {
entry:
  %local = alloca %"kclassbody:Box"
  %f = getelementptr inbounds %"kclassbody:Box", ptr addrspace(1) %obj, i32 0, i32 1
  %v = load ptr addrspace(1), ptr addrspace(1) %f
  %lf = getelementptr inbounds %"kclassbody:Box", ptr %local, i32 0, i32 1
  store ptr addrspace(1) %value, ptr %lf
  ret ptr addrspace(1) %v
}

; CHECK-LABEL: define i32 @derived_phi_of_branch_bases(i1 %c)
; CHECK: merge:
; CHECK-NEXT: %f.base = phi ptr [ %oa, %a ], [ %ob, %b ]
; CHECK-NEXT: %f = phi ptr [ %fa, %a ], [ %fb, %b ]
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %f.base, ptr %slot_0
; CHECK-NEXT: call void @safepoint_1()
define i32 @derived_phi_of_branch_bases(i1 %c) #0 {
entry:
  br i1 %c, label %a, label %b
a:
  %oa = call ptr addrspace(1) @alloc_func()
  %ra = addrspacecast ptr addrspace(1) %oa to ptr
  %fa = getelementptr inbounds i8, ptr %ra, i64 16
  br label %merge
b:
  %ob = call ptr addrspace(1) @alloc_func()
  %rb = addrspacecast ptr addrspace(1) %ob to ptr
  %fb = getelementptr inbounds i8, ptr %rb, i64 24
  br label %merge
merge:
  %f = phi ptr [ %fa, %a ], [ %fb, %b ]
  call void @safepoint_1()
  %v = load i32, ptr %f
  ret i32 %v
}

; CHECK-LABEL: define i32 @derived_select_of_two_objects(i1 %c)
; CHECK: %f.base = select i1 %c, ptr %oa, ptr %ob
; CHECK-NEXT: %slot_0{{[0-9]*}} = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %f.base, ptr %slot_0
; CHECK-NEXT: %f = select i1 %c, ptr %fa, ptr %fb
; CHECK-NEXT: call void @safepoint_1()
define i32 @derived_select_of_two_objects(i1 %c) #0 {
entry:
  %oa = call ptr addrspace(1) @alloc_func()
  %ob = call ptr addrspace(1) @alloc_func()
  %ra = addrspacecast ptr addrspace(1) %oa to ptr
  %rb = addrspacecast ptr addrspace(1) %ob to ptr
  %fa = getelementptr inbounds i8, ptr %ra, i64 16
  %fb = getelementptr inbounds i8, ptr %rb, i64 16
  %f = select i1 %c, ptr %fa, ptr %fb
  call void @safepoint_1()
  %v = load i32, ptr %f
  ret i32 %v
}

; CHECK-LABEL: define i32 @pointer_induction_variable(i64 %n)
; CHECK: %arr = call ptr @alloc_func()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %arr, ptr %slot_0
; CHECK-NOT: .base
; CHECK: ret i32
define i32 @pointer_induction_variable(i64 %n) #0 {
entry:
  %arr = call ptr addrspace(1) @alloc_func()
  %raw = addrspacecast ptr addrspace(1) %arr to ptr
  %first = getelementptr inbounds i8, ptr %raw, i64 16
  br label %loop
loop:
  %p = phi ptr [ %first, %entry ], [ %next, %loop ]
  %i = phi i64 [ 0, %entry ], [ %i.next, %loop ]
  call void @safepoint_1()
  %next = getelementptr inbounds i8, ptr %p, i64 4
  %i.next = add i64 %i, 1
  %done = icmp eq i64 %i.next, %n
  br i1 %done, label %exit, label %loop
exit:
  %v = load i32, ptr %p
  ret i32 %v
}

; CHECK-LABEL: define i32 @loop_carried_merge(i64 %n)
; CHECK: loop:
; CHECK-NEXT: %f.base = phi ptr [ %o0, %entry ], [ %on, %loop ]
; CHECK: store ptr %f.base, ptr %slot_0
; CHECK-NEXT: %on = call ptr @alloc_func()
define i32 @loop_carried_merge(i64 %n) #0 {
entry:
  %o0 = call ptr addrspace(1) @alloc_func()
  %f0 = getelementptr inbounds i8, ptr addrspace(1) %o0, i64 16
  br label %loop
loop:
  %f = phi ptr addrspace(1) [ %f0, %entry ], [ %fn, %loop ]
  %i = phi i64 [ 0, %entry ], [ %i.next, %loop ]
  %on = call ptr addrspace(1) @alloc_func()
  %fn = getelementptr inbounds i8, ptr addrspace(1) %on, i64 16
  %i.next = add i64 %i, 1
  %done = icmp eq i64 %i.next, %n
  br i1 %done, label %exit, label %loop
exit:
  %v = load i32, ptr addrspace(1) %f
  ret i32 %v
}

; CHECK-LABEL: define i32 @merge_dead_before_safepoint(i1 %c)
; CHECK-NOT: EnterFrame
; CHECK-NOT: .base
; CHECK: ret i32
define i32 @merge_dead_before_safepoint(i1 %c) #0 {
entry:
  br i1 %c, label %a, label %b
a:
  %oa = call ptr addrspace(1) @alloc_func()
  %fa = getelementptr inbounds i8, ptr addrspace(1) %oa, i64 16
  br label %merge
b:
  %ob = call ptr addrspace(1) @alloc_func()
  %fb = getelementptr inbounds i8, ptr addrspace(1) %ob, i64 24
  br label %merge
merge:
  %f = phi ptr addrspace(1) [ %fa, %a ], [ %fb, %b ]
  %v = load i32, ptr addrspace(1) %f
  call void @safepoint_1()
  ret i32 %v
}

; CHECK-LABEL: define void @laundered_object_merged_with_null(i1 %c)
; CHECK: %shadow_stack_frame = alloca [3 x ptr], align 8
; CHECK: get:
; CHECK-NEXT: %x = call ptr @raw_source()
; CHECK-NEXT: %slot_0 = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %x, ptr %slot_0
; CHECK-NEXT: call void @use_func(ptr %x)
; CHECK: merge:
; CHECK-NEXT: %p.base = phi ptr [ %x, %get ], [ null, %entry ]
; CHECK-NEXT: %p = phi ptr [ %x, %get ], [ null, %entry ]
; CHECK-NEXT: %slot_0{{[0-9]+}} = getelementptr inbounds [3 x ptr], ptr %shadow_stack_frame, i32 0, i32 2
; CHECK-NEXT: store ptr %p.base, ptr %slot_0
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: call void @raw_call(ptr %p)
define void @laundered_object_merged_with_null(i1 %c) #0 {
entry:
  br i1 %c, label %get, label %merge
get:
  %x = call ptr @raw_source()
  %r = addrspacecast ptr %x to ptr addrspace(1)
  call void @use_func(ptr addrspace(1) %r)
  br label %merge
merge:
  %p = phi ptr [ %x, %get ], [ null, %entry ]
  call void @safepoint_1()
  call void @raw_call(ptr %p)
  ret void
}

; CHECK-LABEL: define void @laundered_object_cast_on_one_path(i1 %c, i1 %d)
; CHECK: get:
; CHECK-NEXT: %x = call ptr @raw_source()
; CHECK-NEXT: br i1 %d
; CHECK: merge:
; CHECK-NEXT: %p.base = phi ptr [ %x, %obj ], [ null, %get ], [ null, %entry ]
; CHECK-NEXT: %p = phi ptr [ %x, %obj ], [ %x, %get ], [ null, %entry ]
; CHECK-NEXT: %slot_{{[0-9]+}} = getelementptr inbounds [{{[0-9]+}} x ptr], ptr %shadow_stack_frame, i32 0, i32 {{[0-9]+}}
; CHECK-NEXT: store ptr %p.base, ptr %slot_
; CHECK-NEXT: call void @safepoint_1()
; CHECK-NEXT: call void @raw_call(ptr %p)
define void @laundered_object_cast_on_one_path(i1 %c, i1 %d) #0 {
entry:
  br i1 %c, label %get, label %merge
get:
  %x = call ptr @raw_source()
  br i1 %d, label %obj, label %merge
obj:
  %r = addrspacecast ptr %x to ptr addrspace(1)
  call void @use_func(ptr addrspace(1) %r)
  br label %merge
merge:
  %p = phi ptr [ %x, %obj ], [ %x, %get ], [ null, %entry ]
  call void @safepoint_1()
  call void @raw_call(ptr %p)
  ret void
}

; CHECK-LABEL: define void @plain_merge_without_reference(i1 %c)
; CHECK-NOT: alloca
; CHECK-NOT: .base
; CHECK: ret void
define void @plain_merge_without_reference(i1 %c) #0 {
entry:
  br i1 %c, label %get, label %merge
get:
  %x = call ptr @raw_source()
  br label %merge
merge:
  %p = phi ptr [ %x, %get ], [ null, %entry ]
  call void @safepoint_1()
  call void @raw_call(ptr %p)
  ret void
}

; CHECK-LABEL: define void @return_slot_objects_merged(i1 %c)
; CHECK: merge:
; CHECK-NEXT: %s.base = phi ptr [ %e, %empty ], [ %h, %alloc ]
; CHECK-NEXT: %s = phi ptr [ %e, %empty ], [ %h, %alloc ]
; CHECK: store ptr %s.base, ptr %slot_
; CHECK: call void @safepoint_1()
; CHECK-NEXT: call void @raw_call(ptr %s)
declare ptr @raw_slot_getter(ptr)

define void @return_slot_objects_merged(i1 %c) #0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  br i1 %c, label %empty, label %alloc
empty:
  %e = call ptr @raw_slot_getter(ptr %slot)
  br label %merge
alloc:
  %a = call ptr @raw_source()
  %h = getelementptr inbounds i8, ptr %a, i64 8
  store ptr %h, ptr %slot
  br label %merge
merge:
  %s = phi ptr [ %e, %empty ], [ %h, %alloc ]
  call void @safepoint_1()
  call void @raw_call(ptr %s)
  ret void
}

; CHECK-LABEL: define void @return_slot_invoke_merged_with_null(ptr %o)
; CHECK: conv:
; CHECK-NEXT: %k = invoke ptr @raw_slot_getter(ptr %slot{{[_0-9]*}})
; CHECK-NEXT: to label %[[SPLIT:.*]] unwind label %lpad
; CHECK: [[SPLIT]]:
; CHECK-NEXT: br label %merge
; CHECK: merge:
; CHECK-NEXT: %p.base = phi ptr [ null, %isnull ], [ %k, %[[SPLIT]] ]
; CHECK-NEXT: %p = phi ptr [ null, %isnull ], [ %k, %[[SPLIT]] ]
; CHECK: store ptr %p.base, ptr %slot_
; CHECK: call void @safepoint_1()
; CHECK-NEXT: call void @raw_call(ptr %p)
declare i32 @__gxx_personality_v0(...)
declare void @Kotlin_gc_frameSetCurrent()

define void @return_slot_invoke_merged_with_null(ptr %o) #0 personality ptr @__gxx_personality_v0 {
entry:
  %slot = call ptr @Kotlin_gc_returnSlot()
  %c = icmp eq ptr %o, null
  br i1 %c, label %isnull, label %conv
isnull:
  store ptr null, ptr %slot
  br label %merge
conv:
  %k = invoke ptr @raw_slot_getter(ptr %slot)
         to label %merge unwind label %lpad
merge:
  %p = phi ptr [ null, %isnull ], [ %k, %conv ]
  call void @safepoint_1()
  call void @raw_call(ptr %p)
  ret void
lpad:
  %lp = landingpad { ptr, i32 } cleanup
  call void @Kotlin_gc_frameSetCurrent()
  resume { ptr, i32 } %lp
}
