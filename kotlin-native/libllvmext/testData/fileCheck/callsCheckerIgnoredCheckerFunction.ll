; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [2 x i8] c"f\00"
; CHECK: @1 = internal constant [17 x i8] c"externalFunction\00"

declare void @externalFunction()

; Runtime is linked in the module. Even though the function is not specially annotated, no
; instrumentation is placed.
; CHECK: define void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr %0, ptr %1, ptr %2) {
define void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr %0, ptr %1, ptr %2) {
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: call void @externalFunction()
  call void @externalFunction()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @f() {
define void @f() {
; inserting call to the checker defined in the current module.
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @1, ptr @externalFunction)
; CHECK-NEXT: call void @externalFunction()
  call void @externalFunction()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}
