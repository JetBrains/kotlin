target datalayout = "e-m:o-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-n32:64-S128-Fn32"

; Prevent function from DCE
@llvm.compiler.used = appending global [1 x i8*] [ i8* bitcast (void (i8*, i8*)* @test to i8*) ], section "llvm.metadata"

declare void @llvm.objc.clang.arc.use(...) nounwind

; define as weak, so we don't have a problem in a two-stage scenario.
define weak void @test(i8* %a, i8* %b) {
  call void (...) @llvm.objc.clang.arc.use(i8* %a, i8* %b) nounwind
  ret void
}