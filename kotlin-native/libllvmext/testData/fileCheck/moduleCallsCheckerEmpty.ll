; OPT: --passes=kotlin-calls-checker-module

; CHECK-NOT: @kotlin.callsChecker.knownFunctions
; CHECK-NOT: @llvm.global_ctors
; CHECK-NOT: @kotlin.callsChecker.module_ctor

declare void @f_declared1()
declare void @f_declared2()
