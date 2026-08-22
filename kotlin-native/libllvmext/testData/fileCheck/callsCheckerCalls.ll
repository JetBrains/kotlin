; OPT: --passes=kotlin-calls-checker

; CHECK: @0 = internal constant [21 x i8] c"callExternalFunction\00"
; CHECK: @1 = internal constant [17 x i8] c"externalFunction\00"
; CHECK: @2 = internal constant [18 x i8] c"callLLVMIntrinsic\00"
; CHECK: @3 = internal constant [15 x i8] c"llvm.donothing\00"
; CHECK: @4 = internal constant [36 x i8] c"callRetainAutoreleasedLLVMIntrinsic\00"
; CHECK: @5 = internal constant [40 x i8] c"llvm.objc.retainAutoreleasedReturnValue\00"
; CHECK: @6 = internal constant [21 x i8] c"callFunctionFromLoad\00"
; CHECK: @7 = internal constant [20 x i8] c"callFunctionFromArg\00"
; CHECK: @8 = internal constant [20 x i8] c"callFunctionFromPhi\00"
; CHECK: @9 = internal constant [23 x i8] c"callFunctionFromSelect\00"
; CHECK: @10 = internal constant [21 x i8] c"callFunctionFromCall\00"
; CHECK: @11 = internal constant [20 x i8] c"callFunctionFromVec\00"
; CHECK: @12 = internal constant [30 x i8] c"callExternalFunctionWithCasts\00"
; CHECK: @13 = internal constant [29 x i8] c"callFunctionFromArgWithCasts\00"

define void @knownFunction() {
  ret void
}

@knownFunctionAlias = alias ptr, ptr @knownFunction

define ptr @knownFunctionCallback() {
  ret ptr @knownFunction
}

declare void @externalFunction()

; CHECK: define void @callKnownFunction() {
define void @callKnownFunction() {
; CHECK-NEXT: call void @knownFunction()
  call void @knownFunction()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callKnownFunctionAlias() {
define void @callKnownFunctionAlias() {
; CHECK-NEXT: call void @knownFunctionAlias()
  call void @knownFunctionAlias()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callExternalFunction() {
define void @callExternalFunction() {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @0, ptr @1, ptr @externalFunction)
; CHECK-NEXT: call void @externalFunction()
  call void @externalFunction()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callLLVMIntrinsic() {
define void @callLLVMIntrinsic() {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @2, ptr @3, ptr inttoptr (i64 -2 to ptr))
; CHECK-NEXT: call void @llvm.donothing()
  call void @llvm.donothing()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callRetainAutoreleasedLLVMIntrinsic(ptr %0) {
define void @callRetainAutoreleasedLLVMIntrinsic(ptr %0) {
; a special case, where instrumentation is placed after the call.
; CHECK-NEXT: %2 = call ptr @llvm.objc.retainAutoreleasedReturnValue(ptr %0)
  %2 = call ptr @llvm.objc.retainAutoreleasedReturnValue(ptr %0)
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @4, ptr @5, ptr inttoptr (i64 -2 to ptr))
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callInlineAsm() {
define void @callInlineAsm() {
; CHECK-NEXT: call void asm "nop", ""()
  call void asm "nop", ""()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromLoad(ptr %0) {
define void @callFunctionFromLoad(ptr %0) {
; CHECK-NEXT: %2 = load ptr, ptr %0
  %2 = load ptr, ptr %0
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @6, ptr null, ptr %2)
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromArg(ptr %0) {
define void @callFunctionFromArg(ptr %0) {
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @7, ptr null, ptr %0)
; CHECK-NEXT: call void %0()
  call void %0()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromPhi(i1 %0) {
define void @callFunctionFromPhi(i1 %0) {
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
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @8, ptr null, ptr %1)
; CHECK-NEXT: call void %1()
  call void %1()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromSelect(i1 %0) {
define void @callFunctionFromSelect(i1 %0) {
; CHECK-NEXT: %2 = select i1 %0, ptr @knownFunction, ptr @externalFunction
  %2 = select i1 %0, ptr @knownFunction, ptr @externalFunction
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @9, ptr null, ptr %2)
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromCall() {
define void @callFunctionFromCall() {
; CHECK-NEXT: %1 = call ptr @knownFunctionCallback()
  %1 = call ptr @knownFunctionCallback()
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @10, ptr null, ptr %1)
; CHECK-NEXT: call void %1()
  call void %1()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromVec(<2 x ptr> %0) {
define void @callFunctionFromVec(<2 x ptr> %0) {
; CHECK-NEXT: %2 = extractelement <2 x ptr> %0, i64 0
  %2 = extractelement <2 x ptr> %0, i64 0
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @11, ptr null, ptr %2)
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callKnownFunctionWithCasts() {
define void @callKnownFunctionWithCasts() {
; CHECK-NEXT: %1 = ptrtoint ptr @knownFunction to i64
  %1 = ptrtoint ptr @knownFunction to i64
; CHECK-NEXT: %2 = inttoptr i64 %1 to ptr
  %2 = inttoptr i64 %1 to ptr
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callExternalFunctionWithCasts() {
define void @callExternalFunctionWithCasts() {
; CHECK-NEXT: %1 = ptrtoint ptr @externalFunction to i64
  %1 = ptrtoint ptr @externalFunction to i64
; CHECK-NEXT: %2 = inttoptr i64 %1 to ptr
  %2 = inttoptr i64 %1 to ptr
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @12, ptr @1, ptr @externalFunction)
; CHECK-NEXT: call void %2()
  call void %2()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: define void @callFunctionFromArgWithCasts(ptr %0) {
define void @callFunctionFromArgWithCasts(ptr %0) {
; CHECK-NEXT: %2 = ptrtoint ptr %0 to i64
  %2 = ptrtoint ptr %0 to i64
; CHECK-NEXT: %3 = inttoptr i64 %2 to ptr
  %3 = inttoptr i64 %2 to ptr
; CHECK-NEXT: call void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr @13, ptr null, ptr %0)
; CHECK-NEXT: call void %3()
  call void %3()
; CHECK-NEXT: ret void
  ret void
; CHECK-NEXT: }{{$}}
}

; CHECK: declare void @Kotlin_mm_checkStateAtExternalFunctionCall(ptr, ptr, ptr)
