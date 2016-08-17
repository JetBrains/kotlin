
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)

target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

@static_area_ptr = global i32 0, align 4
@dynamic_area_ptr = global i32 0, align 4
@static_area = common global [30000 x i8] zeroinitializer, align 1
@dynamic_area = common global [1000 x i8] zeroinitializer, align 1

; Function Attrs: nounwind
define weak void @init_dynamic_area() #0 {
  store i32 0, i32* @dynamic_area_ptr, align 4
  ret void
}

; Function Attrs: nounwind
define weak i8* @malloc_static(i32 %size) #0 {
  %1 = alloca i8*, align 4
  %2 = alloca i32, align 4
  %result = alloca i32, align 4
  store i32 %size, i32* %2, align 4
  %3 = load i32* %2, align 4
  %4 = load i32* @static_area_ptr, align 4
  %5 = add nsw i32 %3, %4
  %6 = icmp sgt i32 %5, 30000
  br i1 %6, label %7, label %8

; <label>:7                                       ; preds = %0
  store i8* null, i8** %1
  br label %17

; <label>:8                                       ; preds = %0
  %9 = load i32* @static_area_ptr, align 4
  %10 = getelementptr inbounds i8* getelementptr inbounds ([30000 x i8]* @static_area, i32 0, i32 0), i32 %9
  %11 = ptrtoint i8* %10 to i32
  store i32 %11, i32* %result, align 4
  %12 = load i32* %2, align 4
  %13 = load i32* @static_area_ptr, align 4
  %14 = add nsw i32 %13, %12
  store i32 %14, i32* @static_area_ptr, align 4
  %15 = load i32* %result, align 4
  %16 = inttoptr i32 %15 to i8*
  store i8* %16, i8** %1
  br label %17

; <label>:17                                      ; preds = %8, %7
  %18 = load i8** %1
  ret i8* %18
}

; Function Attrs: nounwind
define weak i8* @malloc_dynamic(i32 %size) #0 {
  %1 = alloca i8*, align 4
  %2 = alloca i32, align 4
  %result = alloca i32, align 4
  store i32 %size, i32* %2, align 4
  %3 = load i32* %2, align 4
  %4 = load i32* @dynamic_area_ptr, align 4
  %5 = add nsw i32 %3, %4
  %6 = icmp sgt i32 %5, 1000
  br i1 %6, label %7, label %8

; <label>:7                                       ; preds = %0
  store i8* null, i8** %1
  br label %17

; <label>:8                                       ; preds = %0
  %9 = load i32* @dynamic_area_ptr, align 4
  %10 = getelementptr inbounds i8* getelementptr inbounds ([1000 x i8]* @dynamic_area, i32 0, i32 0), i32 %9
  %11 = ptrtoint i8* %10 to i32
  store i32 %11, i32* %result, align 4
  %12 = load i32* %2, align 4
  %13 = load i32* @dynamic_area_ptr, align 4
  %14 = add nsw i32 %13, %12
  store i32 %14, i32* @dynamic_area_ptr, align 4
  %15 = load i32* %result, align 4
  %16 = inttoptr i32 %15 to i8*
  store i8* %16, i8** %1
  br label %17

; <label>:17                                      ; preds = %8, %7
  %18 = load i8** %1
  ret i8* %18
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
