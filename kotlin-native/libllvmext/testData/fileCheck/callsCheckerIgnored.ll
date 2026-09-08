; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [2 x i8] c"f\00"
; CHECK: @1 = internal constant [17 x i8] c"externalFunction\00"
; CHECK: @2 = internal constant [24 x i8] c"externalFunctionIgnored\00"

@value = private unnamed_addr constant [24 x i8] c"no_external_calls_check\00", section "llvm.metadata"
@llvm.global.annotations = appending global [2 x { ptr, ptr, ptr, i32, ptr }] [
    { ptr, ptr, ptr, i32, ptr } { ptr @externalFunctionIgnored, ptr @value, ptr null, i32 0, ptr null },
    { ptr, ptr, ptr, i32, ptr } { ptr @fIgnored, ptr @value, ptr null, i32 0, ptr null }
], section "llvm.metadata"

declare void @externalFunction()
declare void @externalFunctionIgnored()

; CHECK: define void @f() {
define void @f() {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @1, ptr @externalFunction)
; CHECK-NEXT: call void @externalFunction()
  call void @externalFunction()
; the function itself is ignored, but to call it, we need a check.
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @2, ptr @externalFunctionIgnored)
; CHECK-NEXT: call void @externalFunctionIgnored()
  call void @externalFunctionIgnored()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; no instrumentation here
; CHECK: define void @fIgnored() {
define void @fIgnored() {
; CHECK-NEXT: call void @externalFunction()
  call void @externalFunction()
; CHECK-NEXT: call void @externalFunctionIgnored()
  call void @externalFunctionIgnored()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr, ptr, ptr)
