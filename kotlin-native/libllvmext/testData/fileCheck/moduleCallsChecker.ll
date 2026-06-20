; OPT: --passes=kotlin-calls-checker-module

; CHECK: @kotlin.callsChecker.knownFunctions = private constant [2 x ptr] [ptr @f_defined1, ptr @f_defined2]
; CHECK: @llvm.global_ctors = appending global [1 x { i32, ptr, ptr }] [{ i32, ptr, ptr } { i32 0, ptr @kotlin.callsChecker.module_ctor, ptr null }]

; CHECK: define internal void @kotlin.callsChecker.module_ctor() {{.*}} {
; CHECK-NEXT: call void @Kotlin_callsChecker_init(ptr @kotlin.callsChecker.knownFunctions, i64 2)
; CHECK-NEXT: ret void
; CHECK-NEXT: }

define void @f_defined1() {
  ret void
}

define void @f_defined2() {
  ret void
}

declare void @f_declared()
