@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import objc.Answer

// The runtime adds the implementations of the methods of a Kotlin subclass of an Objective-C class with
// class_addMethod, and calls the function that returns its class data, as C function pointers.
// CHECK-DAG: = internal constant [2 x %struct.ObjCMethodDescription] [%struct.ObjCMethodDescription { ptr ptrauth (ptr @{{[^,]+}}, i32 0), ptr @{{[0-9]+}}, ptr @{{[0-9]+}} }, %struct.ObjCMethodDescription { ptr ptrauth (ptr @MissingInitImp, i32 0),
// CHECK-DAG: @"kobjcclassinfo:KotlinAnswer" = {{.*}}ptr ptrauth (ptr @"kobjcclassdataimp:KotlinAnswer#internal", i32 0) }
class KotlinAnswer : Answer() {
    override fun answer(): Int = 2
}
