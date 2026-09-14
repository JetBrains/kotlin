; OPT: --passes=kotlin-calls-checker-module

; Even though there are now known functions, emit good functions, because it could be the only module visible by the runtime
; CHECK: @kotlin.callsChecker.goodFunctionNamesSorted = private constant [[[GOODSIZE:[0-9]+]] x ptr] [{{.*}}]
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSorted = linkonce constant ptr @kotlin.callsChecker.goodFunctionNamesSorted
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSize = linkonce constant i64 [[GOODSIZE]]
; CHECK-NOT: @kotlin.callsChecker.knownFunctions
; CHECK-NOT: @llvm.global_ctors
; CHECK-NOT: @kotlin.callsChecker.module_ctor

declare void @f_declared1()
declare void @f_declared2()
