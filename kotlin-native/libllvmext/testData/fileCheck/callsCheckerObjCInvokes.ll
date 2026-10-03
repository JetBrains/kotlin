; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = private constant [16 x i8] c"invokeSelector0\00"
; CHECK: @1 = private constant [16 x i8] c"invokeSelector1\00"
; CHECK: @2 = private constant [21 x i8] c"invokeSuperSelector0\00"
; CHECK: @3 = private constant [21 x i8] c"invokeSuperSelector1\00"

declare ptr @personality()

declare ptr @objc_msgSend(ptr, ptr, ...)
declare ptr @objc_msgSendSuper2(ptr, ptr, ...)

; CHECK: define ptr @invokeSelector0(ptr %obj, ptr %sel) personality ptr @personality {
define ptr @invokeSelector0(ptr %obj, ptr %sel) personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSend(ptr @0, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = invoke ptr @objc_msgSend(ptr %obj, ptr %sel)
; CHECK-NEXT:         to label %exit unwind label %unwind
  %res = invoke ptr @objc_msgSend(ptr %obj, ptr %sel)
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret ptr null
  ret ptr null

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @invokeSelector1(ptr %obj, ptr %sel, ptr %arg) personality ptr @personality {
define ptr @invokeSelector1(ptr %obj, ptr %sel, ptr %arg) personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSend(ptr @1, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = invoke ptr @objc_msgSend(ptr %obj, ptr %sel, ptr %arg)
; CHECK-NEXT:         to label %exit unwind label %unwind
  %res = invoke ptr @objc_msgSend(ptr %obj, ptr %sel, ptr %arg)
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret ptr null
  ret ptr null

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @invokeSuperSelector0(ptr %obj, ptr %sel) personality ptr @personality {
define ptr @invokeSuperSelector0(ptr %obj, ptr %sel) personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSendSuper2(ptr @2, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = invoke ptr @objc_msgSendSuper2(ptr %obj, ptr %sel)
; CHECK-NEXT:         to label %exit unwind label %unwind
  %res = invoke ptr @objc_msgSendSuper2(ptr %obj, ptr %sel)
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret ptr null
  ret ptr null

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @invokeSuperSelector1(ptr %obj, ptr %sel, ptr %arg) personality ptr @personality {
define ptr @invokeSuperSelector1(ptr %obj, ptr %sel, ptr %arg) personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSendSuper2(ptr @3, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = invoke ptr @objc_msgSendSuper2(ptr %obj, ptr %sel, ptr %arg)
; CHECK-NEXT:         to label %exit unwind label %unwind
  %res = invoke ptr @objc_msgSendSuper2(ptr %obj, ptr %sel, ptr %arg)
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret ptr null
  ret ptr null

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_callsChecker_checkMsgSend(ptr, ptr, ptr)
; CHECK: declare void @Kotlin_callsChecker_checkMsgSendSuper2(ptr, ptr, ptr)
