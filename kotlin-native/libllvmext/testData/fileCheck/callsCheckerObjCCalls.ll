; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = private constant [14 x i8] c"callSelector0\00"
; CHECK: @1 = private constant [14 x i8] c"callSelector1\00"
; CHECK: @2 = private constant [19 x i8] c"callSuperSelector0\00"
; CHECK: @3 = private constant [19 x i8] c"callSuperSelector1\00"

declare ptr @objc_msgSend(ptr, ptr, ...)
declare ptr @objc_msgSendSuper2(ptr, ptr, ...)

; CHECK: define ptr @callSelector0(ptr %obj, ptr %sel) {
define ptr @callSelector0(ptr %obj, ptr %sel) {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSend(ptr @0, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = call ptr @objc_msgSend(ptr %obj, ptr %sel)
  %res = call ptr @objc_msgSend(ptr %obj, ptr %sel)
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @callSelector1(ptr %obj, ptr %sel, ptr %arg) {
define ptr @callSelector1(ptr %obj, ptr %sel, ptr %arg) {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSend(ptr @1, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = call ptr @objc_msgSend(ptr %obj, ptr %sel, ptr %arg)
  %res = call ptr @objc_msgSend(ptr %obj, ptr %sel, ptr %arg)
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @callSuperSelector0(ptr %obj, ptr %sel) {
define ptr @callSuperSelector0(ptr %obj, ptr %sel) {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSendSuper2(ptr @2, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = call ptr @objc_msgSendSuper2(ptr %obj, ptr %sel)
  %res = call ptr @objc_msgSendSuper2(ptr %obj, ptr %sel)
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: define ptr @callSuperSelector1(ptr %obj, ptr %sel, ptr %arg) {
define ptr @callSuperSelector1(ptr %obj, ptr %sel, ptr %arg) {
; CHECK-NEXT: call void @Kotlin_callsChecker_checkMsgSendSuper2(ptr @3, ptr %obj, ptr %sel)
; CHECK-NEXT: %res = call ptr @objc_msgSendSuper2(ptr %obj, ptr %sel, ptr %arg)
  %res = call ptr @objc_msgSendSuper2(ptr %obj, ptr %sel, ptr %arg)
; CHECK-NEXT: ret ptr %res
  ret ptr %res
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_callsChecker_checkMsgSend(ptr, ptr, ptr)
; CHECK: declare void @Kotlin_callsChecker_checkMsgSendSuper2(ptr, ptr, ptr)
