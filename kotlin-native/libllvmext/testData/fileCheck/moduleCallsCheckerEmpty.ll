; OPT: --passes=kotlin-calls-checker-module

; CHECK: @kotlin.callsChecker.goodFunctionNamesSorted = private constant [[[SIZE:[0-9]+]] x ptr]
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSorted = linkonce_odr constant ptr @kotlin.callsChecker.goodFunctionNamesSorted
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSize = linkonce_odr constant i64 [[SIZE]]
; CHECK-NOT: @kotlin.callsChecker.knownFunctions
; CHECK-NOT: @llvm.global_ctors
; CHECK-NOT: @kotlin.callsChecker.module_ctor

declare void @f_declared1()
declare void @f_declared2()
