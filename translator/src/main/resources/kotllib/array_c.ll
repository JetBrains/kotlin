declare i8* @malloc(i32) #1

define i32 @malloc_array(i32 %x) #0 {
  %1 = alloca i32, align 4
  store i32 %x, i32* %1, align 4
  %2 = load i32* %1, align 4
  %3 = call i8* @malloc(i32 %2)
  %4 = ptrtoint i8* %3 to i32
  ret i32 %4
}

; Function Attrs: nounwind uwtable
define signext i8 @kotlinclib_get_byte(i32 %data, i32 %index) {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %ptr = alloca i8*, align 8
  store i32 %data, i32* %1, align 4
  store i32 %index, i32* %2, align 4
  %3 = load i32* %1, align 4
  %4 = sext i32 %3 to i64
  %5 = inttoptr i64 %4 to i8*
  store i8* %5, i8** %ptr, align 8
  %6 = load i8** %ptr, align 8
  %7 = load i32* %2, align 4
  %8 = sext i32 %7 to i64
  %9 = getelementptr inbounds i8* %6, i64 %8
  %10 = load i8* %9, align 1
  ret i8 %10
}

; Function Attrs: nounwind uwtable
define void @kotlinclib_set_byte(i32 %data, i32 %index, i8 signext %value) {
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