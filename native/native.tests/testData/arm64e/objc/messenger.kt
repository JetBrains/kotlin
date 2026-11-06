// C bridges call objc_msgSend through a function pointer.
// CHECK: define {{.*}}@"kfun:#answer(objc.Answer){}kotlin.Int"
// CHECK: select i1 {{.*}}, ptr ptrauth (ptr @objc_msgSend, i32 0), ptr ptrauth (ptr @objc_msgSendSuper, i32 0)

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

fun answer(answer: objc.Answer) = answer.answer()
