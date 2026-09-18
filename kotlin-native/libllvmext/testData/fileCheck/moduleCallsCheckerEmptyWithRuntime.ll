; OPT: --passes=kotlin-calls-checker-module

; Runtime declares these constants
@Kotlin_callsChecker_goodFunctionNamesSorted = external local_unnamed_addr constant ptr, align 8
@Kotlin_callsChecker_goodFunctionNamesSize = external local_unnamed_addr constant i64, align 8

; And the pass overrides them by changing external to linkonce and adding an initializer
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSorted = linkonce local_unnamed_addr constant ptr @kotlin.callsChecker.goodFunctionNamesSorted, align 8
; CHECK: @Kotlin_callsChecker_goodFunctionNamesSize = linkonce local_unnamed_addr constant i64 [[GOODSIZE:[0-9]+]], align 8
; CHECK: @kotlin.callsChecker.goodFunctionNamesSorted = private constant [[[GOODSIZE]] x ptr] [{{.*}}]
