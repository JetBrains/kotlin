; ModuleID = 'console.c'
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@.str = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str1 = private unnamed_addr constant [4 x i8] c"%ld\00", align 1
@.str2 = private unnamed_addr constant [3 x i8] c"%c\00", align 1
@.str3 = private unnamed_addr constant [6 x i8] c"false\00", align 1
@.str4 = private unnamed_addr constant [5 x i8] c"true\00", align 1
@.str5 = private unnamed_addr constant [3 x i8] c"%f\00", align 1
@.str6 = private unnamed_addr constant [4 x i8] c"%lf\00", align 1
@.str7 = private unnamed_addr constant [3 x i8] c"%s\00", align 1

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_int(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str, i32 0, i32 0), i32 %2)
  ret void
}

declare i32 @printf(i8*, ...) #1

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_long(i64 %message) #0 {
  %1 = alloca i64, align 8
  store i64 %message, i64* %1, align 8
  %2 = load i64* %1, align 8
  %3 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([4 x i8]* @.str1, i32 0, i32 0), i64 %2)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_byte(i8 signext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = sext i8 %2 to i32
  %4 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str, i32 0, i32 0), i32 %3)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_short(i16 signext %message) #0 {
  %1 = alloca i16, align 2
  store i16 %message, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = sext i16 %2 to i32
  %4 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str, i32 0, i32 0), i32 %3)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_char(i8 signext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = sext i8 %2 to i32
  %4 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str2, i32 0, i32 0), i32 %3)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_boolean(i1 %message) #0 {
  %1 = alloca i1, align 4
  store i1 %message, i1* %1, align 4
  %2 = load i1* %1, align 4
  %3 = icmp eq i1 %2, 0
  br i1 %3, label %4, label %6

; <label>:4                                       ; preds = %0
  %5 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([6 x i8]* @.str3, i32 0, i32 0))
  br label %8

; <label>:6                                       ; preds = %0
  %7 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([5 x i8]* @.str4, i32 0, i32 0))
  br label %8

; <label>:8                                       ; preds = %6, %4
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_float(float %message) #0 {
  %1 = alloca float, align 4
  store float %message, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fpext float %2 to double
  %4 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str5, i32 0, i32 0), double %3)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_double(double %message) #0 {
  %1 = alloca double, align 8
  store double %message, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([4 x i8]* @.str6, i32 0, i32 0), double %2)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_print_string(i8* %message) #0 {
  %1 = alloca i8*, align 8
  store i8* %message, i8** %1, align 8
  %2 = load i8** %1, align 8
  %3 = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ([3 x i8]* @.str7, i32 0, i32 0), i8* %2)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println() #0 {
  call void @kotlinclib_print_char(i8 signext 10)
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_int(i32 %message) #0 {
  %1 = alloca i32, align 4
  store i32 %message, i32* %1, align 4
  %2 = load i32* %1, align 4
  call void @kotlinclib_print_int(i32 %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_long(i64 %message) #0 {
  %1 = alloca i64, align 8
  store i64 %message, i64* %1, align 8
  %2 = load i64* %1, align 8
  call void @kotlinclib_print_long(i64 %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_byte(i8 signext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  %2 = load i8* %1, align 1
  call void @kotlinclib_print_byte(i8 signext %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_short(i16 signext %message) #0 {
  %1 = alloca i16, align 2
  store i16 %message, i16* %1, align 2
  %2 = load i16* %1, align 2
  call void @kotlinclib_print_short(i16 signext %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_char(i8 signext %message) #0 {
  %1 = alloca i8, align 1
  store i8 %message, i8* %1, align 1
  %2 = load i8* %1, align 1
  call void @kotlinclib_print_char(i8 signext %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_boolean(i1 %message) #0 {
  %1 = alloca i1, align 4
  store i1 %message, i1* %1, align 4
  %2 = load i1* %1, align 4
  call void @kotlinclib_print_boolean(i1 %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_float(float %message) #0 {
  %1 = alloca float, align 4
  store float %message, float* %1, align 4
  %2 = load float* %1, align 4
  call void @kotlinclib_print_float(float %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_double(double %message) #0 {
  %1 = alloca double, align 8
  store double %message, double* %1, align 8
  %2 = load double* %1, align 8
  call void @kotlinclib_print_double(double %2)
  call void @kotlinclib_println()
  ret void
}

; Function Attrs: nounwind uwtable
define weak void @kotlinclib_println_string(i8* %message) #0 {
  %1 = alloca i8*, align 8
  store i8* %message, i8** %1, align 8
  %2 = load i8** %1, align 8
  call void @kotlinclib_print_string(i8* %2)
  call void @kotlinclib_println()
  ret void
}

attributes #0 = { nounwind uwtable "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.ident = !{!0}

!0 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
