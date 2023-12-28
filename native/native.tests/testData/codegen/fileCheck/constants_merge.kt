// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.reflect.*

// CHECK: [[REG_FOR_1024:@[0-9]+]] = internal unnamed_addr constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { ptr {{.*}} }, i32 1024 }
// CHECK-NOT: internal unnamed_addr constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { ptr {{.*}} }, i32 1024 }

// CHECK: internal unnamed_addr constant { %struct.ArrayHeader, [3 x ptr] } { %struct.ArrayHeader { ptr {{.*}}, i32 3 }, [3 x ptr] [ptr [[REG_FOR_1024:@[0-9]+]], ptr [[REG_FOR_2048:@[0-9]+]], ptr [[REG_FOR_4096:@[0-9]+]]] }
// CHECK-NOT: internal unnamed_addr constant { %struct.ArrayHeader, [3 x ptr] } { %struct.ArrayHeader { ptr {{.*}}, i32 3 }, [3 x ptr] [ptr [[REG_FOR_1024]], ptr [[REG_FOR_2048]], ptr [[REG_FOR_4096]]] }

// CHECK: internal unnamed_addr constant { %struct.ObjHeader, ptr, ptr, i1 } { %struct.ObjHeader { ptr {{.*}} }, ptr [[REG_FOR_CLASSIFIER_FIELD:@[0-9]+]], ptr [[REG_FOR_ARGUMENTS_FIELD:@[0-9]+]], i1 false }
// CHECK-NOT: internal unnamed_addr constant { %struct.ObjHeader, ptr, ptr, i1 } { %struct.ObjHeader { ptr {{.*}} }, ptr [[REG_FOR_CLASSIFIER_FIELD]], ptr [[REG_FOR_ARGUMENTS_FIELD]], i1 false }

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(1024)
    println(1024)

    println(arrayOf(1024, 2048, 4096))
    println(arrayOf(1024, 2048, 4096))

    println(typeOf<Map<String, String>>())
    println(typeOf<Map<String, String>>())
    return "OK"
}
