; ModuleID = 'array.c'
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

; Function Attrs: nounwind uwtable
define weak i32 @malloc_array(i32 %x) #0 {
  %1 = alloca i32, align 4
  store i32 %x, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = call i8* @malloc(i32 %2)
  %4 = ptrtoint i8* %3 to i32
  ret i32 %4
}

declare i8* @malloc(i32) #1

; Function Attrs: nounwind uwtable
define weak signext i8 @kotlinclib_get_byte(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = sext i32 %3 to i64
  %5 = inttoptr i64 %4 to i8*
  %6 = load i32* %2, align 4
  %7 = sext i32 %6 to i64
  %8 = getelementptr inbounds i8* %5, i64 %7
  %9 = load i8* %8, align 1
  ret i8 %9
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_set_byte(i32 %data, i32 %index, i8 signext %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i8, align 1
  %ptr = alloca i8*, align 8
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i8 %value, i8* %3, align 1
  %4 = load i32* %1, align 4
  %5 = sext i32 %4 to i64
  %6 = inttoptr i64 %5 to i8*
  store i8* %6, i8** %ptr, align 8
  %7 = load i8* %3, align 1
  %8 = load i8** %ptr, align 8
  %9 = load i32* %2, align 4
  %10 = sext i32 %9 to i64
  %11 = getelementptr inbounds i8* %8, i64 %10
  store i8 %7, i8* %11, align 1
  ret void
}

; Function Attrs: nounwind uwtable
define weak i32 @kotlinclib_get_int(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = sext i32 %3 to i64
  %5 = inttoptr i64 %4 to i32*
  %6 = load i32* %2, align 4
  %7 = sext i32 %6 to i64
  %8 = getelementptr inbounds i32* %5, i64 %7
  %9 = load i32* %8, align 4
  ret i32 %9
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_set_int(i32 %data, i32 %index, i32 %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %ptr = alloca i32*, align 8
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i32 %value, i32* %3, align 4
  %4 = load i32* %1, align 4
  %5 = sext i32 %4 to i64
  %6 = inttoptr i64 %5 to i32*
  store i32* %6, i32** %ptr, align 8
  %7 = load i32* %3, align 4
  %8 = load i32** %ptr, align 8
  %9 = load i32* %2, align 4
  %10 = sext i32 %9 to i64
  %11 = getelementptr inbounds i32* %8, i64 %10
  store i32 %7, i32* %11, align 4
  ret void
}

; Function Attrs: nounwind uwtable
define weak signext i16 @kotlinclib_get_short(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = sext i32 %3 to i64
  %5 = inttoptr i64 %4 to i16*
  %6 = load i32* %2, align 4
  %7 = sext i32 %6 to i64
  %8 = getelementptr inbounds i16* %5, i64 %7
  %9 = load i16* %8, align 2
  ret i16 %9
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_set_short(i32 %data, i32 %index, i16 signext %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i16, align 2
  %ptr = alloca i16*, align 8
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i16 %value, i16* %3, align 2
  %4 = load i32* %1, align 4
  %5 = sext i32 %4 to i64
  %6 = inttoptr i64 %5 to i16*
  store i16* %6, i16** %ptr, align 8
  %7 = load i16* %3, align 2
  %8 = load i16** %ptr, align 8
  %9 = load i32* %2, align 4
  %10 = sext i32 %9 to i64
  %11 = getelementptr inbounds i16* %8, i64 %10
  store i16 %7, i16* %11, align 2
  ret void
}

; Function Attrs: nounwind uwtable
define weak i64 @kotlinclib_get_long(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = sext i32 %3 to i64
  %5 = inttoptr i64 %4 to i64*
  %6 = load i32* %2, align 4
  %7 = sext i32 %6 to i64
  %8 = getelementptr inbounds i64* %5, i64 %7
  %9 = load i64* %8, align 8
  ret i64 %9
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_set_long(i32 %data, i32 %index, i64 %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i64, align 8
  %ptr = alloca i64*, align 8
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i64 %value, i64* %3, align 8
  %4 = load i32* %1, align 4
  %5 = sext i32 %4 to i64
  %6 = inttoptr i64 %5 to i64*
  store i64* %6, i64** %ptr, align 8
  %7 = load i64* %3, align 8
  %8 = load i64** %ptr, align 8
  %9 = load i32* %2, align 4
  %10 = sext i32 %9 to i64
  %11 = getelementptr inbounds i64* %8, i64 %10
  store i64 %7, i64* %11, align 8
  ret void
}

attributes #0 = { nounwind uwtable "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.ident = !{!0}

!0 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
