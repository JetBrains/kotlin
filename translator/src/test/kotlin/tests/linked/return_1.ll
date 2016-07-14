declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
define i32 @return_test_1(i32  %x) 
{
%x.addr = alloca i32, align 4
store i32 %x, i32* %x.addr, align 4
ret i32 101
}

