; OPT: --passes=kotlin-remove-sp<inline>

@g = global i32 0

declare void @Kotlin_mm_safePointFunctionPrologue()

; CHECK: define void @singleBB() {
define void @singleBB() {
  %1 = load i32, ptr @g
  ; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
  ; CHECK-NOT: call void @Kotlin_mm_safePointFunctionPrologue()
  call void @Kotlin_mm_safePointFunctionPrologue()
  %2 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  ret void
  ; CHECK: }{{$}}
}

; CHECK: define void @multipleBBs_keepOnlyFirst() {
define void @multipleBBs_keepOnlyFirst() {
entry:
  %1 = load i32, ptr @g
  ; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
  ; CHECK-NOT: call void @Kotlin_mm_safePointFunctionPrologue()
  call void @Kotlin_mm_safePointFunctionPrologue()
  %2 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  %if.cond = icmp eq i32 %2, 0
  br i1 %if.cond, label %if.then, label %if.else

if.then:
  %3 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  %4 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %exit

if.else:
  %5 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  %6 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %exit

exit:
  %7 = phi i32 [ %4, %if.then ], [ %6, %if.else ]
  call void @Kotlin_mm_safePointFunctionPrologue()
  %8 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  ret void
  ; CHECK: }{{$}}
}

; CHECK: define void @multipleBBs_keepFirstInEveryBB() {
define void @multipleBBs_keepFirstInEveryBB() {
entry:
  %1 = load i32, ptr @g
  %if.cond = icmp eq i32 %1, 0
  br i1 %if.cond, label %if.then, label %if.else

if.then:
  %2 = load i32, ptr @g
  ; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
  ; CHECK-NOT: call void @Kotlin_mm_safePointFunctionPrologue()
  call void @Kotlin_mm_safePointFunctionPrologue()
  %3 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %exit

if.else:
  %4 = load i32, ptr @g
  ; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
  ; CHECK-NOT: call void @Kotlin_mm_safePointFunctionPrologue()
  call void @Kotlin_mm_safePointFunctionPrologue()
  %5 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  br label %exit

exit:
  %6 = phi i32 [ %3, %if.then ], [ %5, %if.else ]
  ; CHECK: call void @Kotlin_mm_safePointFunctionPrologue()
  ; CHECK-NOT: call void @Kotlin_mm_safePointFunctionPrologue()
  call void @Kotlin_mm_safePointFunctionPrologue()
  %7 = load i32, ptr @g
  call void @Kotlin_mm_safePointFunctionPrologue()
  ret void
  ; CHECK: }{{$}}
}
