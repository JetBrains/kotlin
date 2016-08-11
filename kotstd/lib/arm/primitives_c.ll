; ModuleID = 'primitives.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_intToByte(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_intToChar(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_intToShort(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_intToLong(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  ret i32 %2
}

; Function Attrs: nounwind
define weak float @kotlinclib_intToFloat(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = sitofp i32 %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_intToDouble(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = sitofp i32 %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_byteToChar(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  ret i8 %2
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_byteToShort(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_byteToInt(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_byteToLong(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak float @kotlinclib_byteToFloat(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = uitofp i8 %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_byteToDouble(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = uitofp i8 %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_charToByte(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  ret i8 %2
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_charToShort(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_charToInt(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_charToLong(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = zext i8 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak float @kotlinclib_charToFloat(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = uitofp i8 %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_charToDouble(i8 zeroext %value) #0 {
  %1 = alloca i8, align 1
  store i8 %value, i8* %1, align 1
  %2 = load i8* %1, align 1
  %3 = uitofp i8 %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_shortToByte(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = trunc i16 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_shortToChar(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = trunc i16 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_shortToInt(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = sext i16 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_shortToLong(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = sext i16 %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak float @kotlinclib_shortToFloat(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = sitofp i16 %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_shortToDouble(i16 signext %value) #0 {
  %1 = alloca i16, align 2
  store i16 %value, i16* %1, align 2
  %2 = load i16* %1, align 2
  %3 = sitofp i16 %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_longToByte(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_longToChar(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_longToShort(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = trunc i32 %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_longToInt(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  ret i32 %2
}

; Function Attrs: nounwind
define weak float @kotlinclib_longToFloat(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = sitofp i32 %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_longToDouble(i32 %value) #0 {
  %1 = alloca i32, align 4
  store i32 %value, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = sitofp i32 %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_floatToByte(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fptoui float %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_floatToChar(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fptoui float %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_floatToShort(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fptosi float %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_floatToInt(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fptosi float %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_floatToLong(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fptosi float %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak double @kotlinclib_floatToDouble(float %value) #0 {
  %1 = alloca float, align 4
  store float %value, float* %1, align 4
  %2 = load float* %1, align 4
  %3 = fpext float %2 to double
  ret double %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_doubleToByte(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptoui double %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak zeroext i8 @kotlinclib_doubleToChar(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptoui double %2 to i8
  ret i8 %3
}

; Function Attrs: nounwind
define weak signext i16 @kotlinclib_doubleToShort(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptosi double %2 to i16
  ret i16 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_doubleToInt(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptosi double %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_doubleToLong(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptosi double %2 to i32
  ret i32 %3
}

; Function Attrs: nounwind
define weak float @kotlinclib_doubleToFloat(double %value) #0 {
  %1 = alloca double, align 8
  store double %value, double* %1, align 8
  %2 = load double* %1, align 8
  %3 = fptrunc double %2 to float
  ret float %3
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_int_size() #0 {
  ret i32 4
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_long_size() #0 {
  ret i32 8
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_boolean_size() #0 {
  ret i32 1
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_short_size() #0 {
  ret i32 2
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_double_size() #0 {
  ret i32 8
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_float_size() #0 {
  ret i32 4
}

; Function Attrs: nounwind
define weak i32 @kotlinclib_char_size() #0 {
  ret i32 1
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0, !1}
!llvm.ident = !{!2}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 1, !"min_enum_size", i32 4}
!2 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
