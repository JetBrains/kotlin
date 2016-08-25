; ModuleID = 'array.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

; Function Attrs: nounwind
define weak i32 @kotlin.malloc_array(i32 %x) #0 {
  %1 = alloca i32, align 4
  store i32 %x, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = call i8* @malloc_heap(i32 %2) #2
  %4 = ptrtoint i8* %3 to i32
  ret i32 %4
}

declare i8* @malloc_heap(i32) #1

; Function Attrs: nounwind
define weak zeroext i8 @kotlin.kotlinclib_get_byte(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = inttoptr i32 %3 to i8*
  %5 = load i32* %2, align 4
  %6 = getelementptr inbounds i8* %4, i32 %5
  %7 = load i8* %6, align 1
  ret i8 %7
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_set_byte(i32 %data, i32 %index, i8 zeroext %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i8, align 1
  %ptr = alloca i8*, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i8 %value, i8* %3, align 1
  %4 = load i32* %1, align 4
  %5 = inttoptr i32 %4 to i8*
  store i8* %5, i8** %ptr, align 4
  %6 = load i8* %3, align 1
  %7 = load i8** %ptr, align 4
  %8 = load i32* %2, align 4
  %9 = getelementptr inbounds i8* %7, i32 %8
  store i8 %6, i8* %9, align 1
  ret void
}

; Function Attrs: nounwind
define weak i32 @kotlin.kotlinclib_get_int(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = inttoptr i32 %3 to i32*
  %5 = load i32* %2, align 4
  %6 = getelementptr inbounds i32* %4, i32 %5
  %7 = load i32* %6, align 4
  ret i32 %7
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_set_int(i32 %data, i32 %index, i32 %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %ptr = alloca i32*, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i32 %value, i32* %3, align 4
  %4 = load i32* %1, align 4
  %5 = inttoptr i32 %4 to i32*
  store i32* %5, i32** %ptr, align 4
  %6 = load i32* %3, align 4
  %7 = load i32** %ptr, align 4
  %8 = load i32* %2, align 4
  %9 = getelementptr inbounds i32* %7, i32 %8
  store i32 %6, i32* %9, align 4
  ret void
}

; Function Attrs: nounwind
define weak signext i16 @kotlin.kotlinclib_get_short(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = inttoptr i32 %3 to i16*
  %5 = load i32* %2, align 4
  %6 = getelementptr inbounds i16* %4, i32 %5
  %7 = load i16* %6, align 2
  ret i16 %7
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_set_short(i32 %data, i32 %index, i16 signext %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i16, align 2
  %ptr = alloca i16*, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i16 %value, i16* %3, align 2
  %4 = load i32* %1, align 4
  %5 = inttoptr i32 %4 to i16*
  store i16* %5, i16** %ptr, align 4
  %6 = load i16* %3, align 2
  %7 = load i16** %ptr, align 4
  %8 = load i32* %2, align 4
  %9 = getelementptr inbounds i16* %7, i32 %8
  store i16 %6, i16* %9, align 2
  ret void
}

; Function Attrs: nounwind
define weak i32 @kotlin.kotlinclib_get_long(i32 %data, i32 %index) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = inttoptr i32 %3 to i32*
  %5 = load i32* %2, align 4
  %6 = getelementptr inbounds i32* %4, i32 %5
  %7 = load i32* %6, align 4
  ret i32 %7
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_set_long(i32 %data, i32 %index, i32 %value) #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %ptr = alloca i32*, align 4
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  store i32 %value, i32* %3, align 4
  %4 = load i32* %1, align 4
  %5 = inttoptr i32 %4 to i32*
  store i32* %5, i32** %ptr, align 4
  %6 = load i32* %3, align 4
  %7 = load i32** %ptr, align 4
  %8 = load i32* %2, align 4
  %9 = getelementptr inbounds i32* %7, i32 %8
  store i32 %6, i32* %9, align 4
  ret void
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { nobuiltin }
