// A Kotlin function passed to Objective-C is converted to a block. It is signed as Clang signs a stack block: the isa
// pointer with the DA key, its address and the constant 27361, the invoke pointer and the copy and dispose helpers
// with the IA key and their addresses.

// CHECK-DAG: @[[DESCRIPTOR:[0-9]+]] = internal global %struct.Block_descriptor_1 { i64 0, i64 {{[0-9]+}}, ptr ptrauth (ptr @blockCopyHelper, i32 0, i64 0, ptr getelementptr inbounds{{( nuw)?}} (%struct.Block_descriptor_1, ptr @[[DESCRIPTOR]], i32 0, i32 2)), ptr ptrauth (ptr @blockDisposeHelper, i32 0, i64 0, ptr getelementptr inbounds{{( nuw)?}} (%struct.Block_descriptor_1, ptr @[[DESCRIPTOR]], i32 0, i32 3)),
fun makeBlock(): () -> Any = { "block" }

// CHECK-LABEL: define internal ptr @convertFunction0(ptr %0)
// CHECK: %[[ISA_ADDRESS:[0-9]+]] = ptrtoint ptr %{{[0-9]+}} to i64
// CHECK-NEXT: %[[ISA_DISCRIMINATOR:[0-9]+]] = call i64 @llvm.ptrauth.blend(i64 %[[ISA_ADDRESS]], i64 27361)
// CHECK-NEXT: call i64 @llvm.ptrauth.sign(i64 ptrtoint (ptr @_NSConcreteStackBlock to i64), i32 immarg 2, i64 %[[ISA_DISCRIMINATOR]])
// CHECK: %[[INVOKE_ADDRESS:[0-9]+]] = ptrtoint ptr %{{[0-9]+}} to i64
// CHECK-NEXT: call i64 @llvm.ptrauth.sign(i64 ptrtoint (ptr @invokeBlock0 to i64), i32 immarg 0, i64 %[[INVOKE_ADDRESS]])

// A block received from Objective-C is called with its invoke pointer authenticated by the pointer's address. The call
// is outlined to retain the autoreleased result, so the address is passed to the outlined function.
// CHECK-LABEL: define internal ptr @invokeFunction0(ptr %0, ptr %1)
// CHECK: %[[INVOKE_FIELD:[0-9]+]] = getelementptr inbounds{{( nuw)?}} %struct.Block_literal_1, ptr %{{[0-9]+}}, i32 0, i32 3
// CHECK: %[[INVOKE_DISCRIMINATOR:[0-9]+]] = ptrtoint ptr %[[INVOKE_FIELD]] to i64
// CHECK: {{call|invoke}} ptr @invokeFunction0_outlined(ptr %{{[0-9]+}}, ptr %{{[0-9]+}}, i64 %[[INVOKE_DISCRIMINATOR]])
// CHECK-LABEL: define private ptr @invokeFunction0_outlined(ptr %0, ptr %1, i64 %2)
// CHECK: call ptr %1(ptr %0) [ "ptrauth"(i32 0, i64 %2) ]
fun callBlock(block: () -> Any) = block()
