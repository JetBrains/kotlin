
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32) #0

define weak i8* @malloc_static(i32 %size) #0 {
    %1 = alloca i32, align 4
    store i32 %size, i32* %1, align 4
    %2 = load i32* %1, align 4
    %3 = call i8* @malloc(i32 %2)
    ret i8* %3
}
