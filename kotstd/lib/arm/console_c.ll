; ModuleID = 'console_arm.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_int(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_long(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_byte(i8 zeroext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_short(i16 signext %message) #0 {
  %1 = alloca i16, align 2
  store i16 %message, i16* %1, align 2
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_char(i8 zeroext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_boolean(i1 %message) #0 {
  %1 = alloca i1, align 4
  store i1 %message, i1* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_float(float %message) #0 {
  %1 = alloca float, align 4
  store float %message, float* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_double(double %message) #0 {
  %1 = alloca double, align 8
  store double %message, double* %1, align 8
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_print_string(i8* %message) #0 {
  %1 = alloca i8*, align 4
  store i8* %message, i8** %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_int(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_long(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_byte(i8 zeroext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_short(i16 signext %message) #0 {
  %1 = alloca i16, align 2
  store i16 %message, i16* %1, align 2
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_char(i8 zeroext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_boolean(i1 %message) #0 {
  %1 = alloca i1, align 4
  store i1 %message, i1* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_float(float %message) #0 {
  %1 = alloca float, align 4
  store float %message, float* %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_double(double %message) #0 {
  %1 = alloca double, align 8
  store double %message, double* %1, align 8
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println_string(i8* %message) #0 {
  %1 = alloca i8*, align 4
  store i8* %message, i8** %1, align 4
  ret void
}

; Function Attrs: nounwind
define weak void @kotlin.kotlinclib_println() #0 {
  ret void
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
