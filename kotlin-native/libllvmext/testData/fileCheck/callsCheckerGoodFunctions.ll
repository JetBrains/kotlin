; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [2 x i8] c"f\00"
; CHECK: @1 = internal constant [7 x i8] c"sinBad\00"

; Runtime is linked in the module.
@.str.0 = private unnamed_addr constant [4 x i8] c"sin\00", align 1
@.str.1 = private unnamed_addr constant [11 x i8] c"llvm.sin.*\00", align 1
@Kotlin_callsCheckerGoodFunctionNames = local_unnamed_addr global [2 x ptr] [ptr @.str.0, ptr @.str.1], align 8

declare double @sin(double)
declare double @sinBad(double)

; CHECK: define void @f(double %0) {
define void @f(double %0) {
; sin is a good function, call uninstrumented
; CHECK-NEXT: call double @sin(double %0)
  call double @sin(double %0)
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @1, ptr @sinBad)
; CHECK-NEXT: call double @sinBad(double %0)
  call double @sinBad(double %0)
; llvm.sin.* is a good intrinsics family, call uninstrumented
; CHECK-NEXT: call double @llvm.sin.f64(double %0)
  call double @llvm.sin.f64(double %0)
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr, ptr, ptr)
