; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [23 x i8] c"invokeExternalFunction\00"
; CHECK: @1 = internal constant [17 x i8] c"externalFunction\00"
; CHECK: @2 = internal constant [20 x i8] c"invokeLLVMIntrinsic\00"
; CHECK: @3 = internal constant [15 x i8] c"llvm.donothing\00"
; CHECK: @4 = internal constant [23 x i8] c"invokeFunctionFromLoad\00"
; CHECK: @5 = internal constant [22 x i8] c"invokeFunctionFromArg\00"
; CHECK: @6 = internal constant [22 x i8] c"invokeFunctionFromPhi\00"
; CHECK: @7 = internal constant [25 x i8] c"invokeFunctionFromSelect\00"
; CHECK: @8 = internal constant [23 x i8] c"invokeFunctionFromCall\00"
; CHECK: @9 = internal constant [22 x i8] c"invokeFunctionFromVec\00"
; CHECK: @10 = internal constant [32 x i8] c"invokeExternalFunctionWithCasts\00"
; CHECK: @11 = internal constant [31 x i8] c"invokeFunctionFromArgWithCasts\00"

declare ptr @personality()

define void @knownFunction() {
  ret void
}

@knownFunctionAlias = alias ptr, ptr @knownFunction

define ptr @knownFunctionCallback() {
  ret ptr @knownFunction
}

declare void @externalFunction()

; CHECK: define void @invokeKnownFunction() personality ptr @personality {
define void @invokeKnownFunction() personality ptr @personality {
; CHECK-NEXT: invoke void @knownFunction()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void @knownFunction()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeKnownFunctionAlias() personality ptr @personality {
define void @invokeKnownFunctionAlias() personality ptr @personality {
; CHECK-NEXT: invoke void @knownFunctionAlias()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void @knownFunctionAlias()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeExternalFunction() personality ptr @personality {
define void @invokeExternalFunction() personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @1, ptr @externalFunction)
; CHECK-NEXT: invoke void @externalFunction()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void @externalFunction()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeLLVMIntrinsic() personality ptr @personality {
define void @invokeLLVMIntrinsic() personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @2, ptr @3, ptr inttoptr (i64 -2 to ptr))
; CHECK-NEXT: invoke void @llvm.donothing()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void @llvm.donothing()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeInlineAsm() personality ptr @personality {
define void @invokeInlineAsm() personality ptr @personality {
; CHECK-NEXT: invoke void asm "nop", ""()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void asm "nop", ""()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromLoad(ptr %0) personality ptr @personality {
define void @invokeFunctionFromLoad(ptr %0) personality ptr @personality {
; CHECK-NEXT: %2 = load ptr, ptr %0
  %2 = load ptr, ptr %0
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @4, ptr null, ptr %2)
; CHECK-NEXT: invoke void %2()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %2()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromArg(ptr %0) personality ptr @personality {
define void @invokeFunctionFromArg(ptr %0) personality ptr @personality {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @5, ptr null, ptr %0)
; CHECK-NEXT: invoke void %0()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %0()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromPhi(i1 %0) personality ptr @personality {
define void @invokeFunctionFromPhi(i1 %0) personality ptr @personality {
; CHECK-NEXT: entry:
entry:
; CHECK-NEXT: br i1 %0, label %t, label %f
  br i1 %0, label %t, label %f

; CHECK-EMPTY:
; CHECK-NEXT: t:
t:
; CHECK-NEXT: br label %next
  br label %next

; CHECK-EMPTY:
; CHECK-NEXT: f:
f:
; CHECK-NEXT: br label %next
  br label %next

; CHECK-EMPTY:
; CHECK-NEXT: next:
next:
; CHECK-NEXT: %1 = phi ptr [ @knownFunction, %t ], [ @externalFunction, %f ]
  %1 = phi ptr [ @knownFunction, %t ], [ @externalFunction, %f ]
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @6, ptr null, ptr %1)
; CHECK-NEXT: invoke void %1()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %1()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromSelect(i1 %0) personality ptr @personality {
define void @invokeFunctionFromSelect(i1 %0) personality ptr @personality {
; CHECK-NEXT: %2 = select i1 %0, ptr @knownFunction, ptr @externalFunction
  %2 = select i1 %0, ptr @knownFunction, ptr @externalFunction
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @7, ptr null, ptr %2)
; CHECK-NEXT: invoke void %2()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %2()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromCall() personality ptr @personality {
define void @invokeFunctionFromCall() personality ptr @personality {
; CHECK-NEXT: %1 = call ptr @knownFunctionCallback()
  %1 = call ptr @knownFunctionCallback()
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @8, ptr null, ptr %1)
; CHECK-NEXT: invoke void %1()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %1()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromVec(<2 x ptr> %0) personality ptr @personality {
define void @invokeFunctionFromVec(<2 x ptr> %0) personality ptr @personality {
; CHECK-NEXT: %2 = extractelement <2 x ptr> %0, i64 0
  %2 = extractelement <2 x ptr> %0, i64 0
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @9, ptr null, ptr %2)
; CHECK-NEXT: invoke void %2()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %2()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeKnownFunctionWithCasts() personality ptr @personality {
define void @invokeKnownFunctionWithCasts() personality ptr @personality {
; CHECK-NEXT: %1 = ptrtoint ptr @knownFunction to i64
  %1 = ptrtoint ptr @knownFunction to i64
; CHECK-NEXT: %2 = inttoptr i64 %1 to ptr
  %2 = inttoptr i64 %1 to ptr
; CHECK-NEXT: invoke void %2()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %2()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeExternalFunctionWithCasts() personality ptr @personality {
define void @invokeExternalFunctionWithCasts() personality ptr @personality {
; CHECK-NEXT: %1 = ptrtoint ptr @externalFunction to i64
  %1 = ptrtoint ptr @externalFunction to i64
; CHECK-NEXT: %2 = inttoptr i64 %1 to ptr
  %2 = inttoptr i64 %1 to ptr
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @10, ptr @1, ptr @externalFunction)
; CHECK-NEXT: invoke void %2()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %2()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @invokeFunctionFromArgWithCasts(ptr %0) personality ptr @personality {
define void @invokeFunctionFromArgWithCasts(ptr %0) personality ptr @personality {
; CHECK-NEXT: %2 = ptrtoint ptr %0 to i64
  %2 = ptrtoint ptr %0 to i64
; CHECK-NEXT: %3 = inttoptr i64 %2 to ptr
  %3 = inttoptr i64 %2 to ptr
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @11, ptr null, ptr %0)
; CHECK-NEXT: invoke void %3()
; CHECK-NEXT:         to label %exit unwind label %unwind
  invoke void %3()
          to label %exit unwind label %unwind

; CHECK-EMPTY:
; CHECK-NEXT: unwind:
unwind:
; CHECK-NEXT: %e = landingpad ptr
; CHECK-NEXT:         cleanup
  %e = landingpad ptr
          cleanup
; CHECK-NEXT: ret void
  ret void

; CHECK-EMPTY:
; CHECK-NEXT: exit:
exit:
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr, ptr, ptr)
