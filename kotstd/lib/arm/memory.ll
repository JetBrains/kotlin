; ModuleID = '/home/user/Documents/carkot/kotstd/libc/memory.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

@static_area = common global [30000 x i8] zeroinitializer, align 1
@dynamic_area = common global [30000 x i8] zeroinitializer, align 1
@heaps = global [2 x i8*] [i8* getelementptr inbounds ([30000 x i8]* @static_area, i32 0, i32 0), i8* getelementptr inbounds ([30000 x i8]* @dynamic_area, i32 0, i32 0)], align 4
@heap_tails = global [2 x i32] zeroinitializer, align 4
@active_heap = global i32 0, align 4

; Function Attrs: nounwind
define i8* @malloc_heap(i32 %size) #0 {
  %1 = alloca i32, align 4
  %ptr = alloca i8*, align 4
  store i32 %size, i32* %1, align 4
  %2 = load i32* @active_heap, align 4
  %3 = getelementptr inbounds [2 x i8*]* @heaps, i32 0, i32 %2
  %4 = load i8** %3, align 4
  %5 = load i32* @active_heap, align 4
  %6 = getelementptr inbounds [2 x i32]* @heap_tails, i32 0, i32 %5
  %7 = load i32* %6, align 4
  %8 = getelementptr inbounds i8* %4, i32 %7
  store i8* %8, i8** %ptr, align 4
  %9 = load i32* %1, align 4
  %10 = load i32* @active_heap, align 4
  %11 = getelementptr inbounds [2 x i32]* @heap_tails, i32 0, i32 %10
  %12 = load i32* %11, align 4
  %13 = add nsw i32 %12, %9
  store i32 %13, i32* %11, align 4
  %14 = load i8** %ptr, align 4
  ret i8* %14
}

; Function Attrs: nounwind
define void @set_active_heap(i32 %heap) #0 {
  %1 = alloca i32, align 4
  store i32 %heap, i32* %1, align 4
  %2 = load i32* %1, align 4
  store i32 %2, i32* @active_heap, align 4
  ret void
}

; Function Attrs: nounwind
define void @clean_dynamic_heap() #0 {
  store i32 0, i32* getelementptr inbounds ([2 x i32]* @heap_tails, i32 0, i32 1), align 4
  ret void
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0, !1}
!llvm.ident = !{!2}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 1, !"min_enum_size", i32 4}
!2 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
