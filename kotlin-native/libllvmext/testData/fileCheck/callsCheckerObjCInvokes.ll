; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [36 x i8] c"invokeSelector0 (over objc_msgSend)\00"
; CHECK: @1 = internal constant [36 x i8] c"invokeSelector1 (over objc_msgSend)\00"
; CHECK: @2 = internal constant [47 x i8] c"invokeSuperSelector0 (over objc_msgSendSuper2)\00"
; CHECK: @3 = internal constant [47 x i8] c"invokeSuperSelector1 (over objc_msgSendSuper2)\00"

declare ptr @personality()

declare ptr @objc_msgSend(ptr, ptr, ...)
declare ptr @objc_msgSendSuper2(ptr, ptr, ...)

; CHECK: define ptr @invokeSelector0(ptr %obj, ptr %sel) personality ptr @personality {
define ptr @invokeSelector0(ptr %obj, ptr %sel) personality ptr @personality {
; CHECK-NEXT: %1 = call ptr @object_getClass(ptr %obj)
; CHECK-NEXT: %2 = icmp eq ptr %obj, null
; CHECK-NEXT: %3 = call ptr @class_getMethodImplementation(ptr %1, ptr %sel)
; CHECK-NEXT: %4 = select i1 %2, ptr inttoptr (i64 -1 to ptr), ptr %3
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr null, ptr %4)
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
; CHECK-NEXT: %1 = call ptr @object_getClass(ptr %obj)
; CHECK-NEXT: %2 = icmp eq ptr %obj, null
; CHECK-NEXT: %3 = call ptr @class_getMethodImplementation(ptr %1, ptr %sel)
; CHECK-NEXT: %4 = select i1 %2, ptr inttoptr (i64 -1 to ptr), ptr %3
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @1, ptr null, ptr %4)
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
; CHECK-NEXT: %1 = getelementptr inbounds nuw { ptr, ptr }, ptr %obj, i32 0, i32 1
; CHECK-NEXT: %2 = load ptr, ptr %1, align 8
; CHECK-NEXT: %3 = call ptr @class_getSuperclass(ptr %2)
; CHECK-NEXT: %4 = call ptr @class_getMethodImplementation(ptr %3, ptr %sel)
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @2, ptr null, ptr %4)
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
; CHECK-NEXT: %1 = getelementptr inbounds nuw { ptr, ptr }, ptr %obj, i32 0, i32 1
; CHECK-NEXT: %2 = load ptr, ptr %1, align 8
; CHECK-NEXT: %3 = call ptr @class_getSuperclass(ptr %2)
; CHECK-NEXT: %4 = call ptr @class_getMethodImplementation(ptr %3, ptr %sel)
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @3, ptr null, ptr %4)
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

; CHECK: declare void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr, ptr, ptr)
; CHECK: declare ptr @class_getMethodImplementation(ptr, ptr)
; CHECK: declare ptr @object_getClass(ptr)
; CHECK: declare ptr @class_getSuperclass(ptr)
