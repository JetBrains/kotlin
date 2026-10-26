// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.reflect.*

// CHECK: [[REG_FOR_1024:@[0-9]+]] = internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { ptr{{( addrspace\(1\))?}} {{.*}} }, i32 1024 }
// CHECK-NOT: internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { ptr{{( addrspace\(1\))?}} {{.*}} }, i32 1024 }

// CHECK: internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ArrayHeader, [3 x ptr{{( addrspace\(1\))?}}] } { %struct.ArrayHeader { ptr{{( addrspace\(1\))?}} {{.*}}, i32 3 }, [3 x ptr{{( addrspace\(1\))?}}] [ptr{{( addrspace\(1\))?}} [[REG_FOR_1024:@[0-9]+]], ptr{{( addrspace\(1\))?}} [[REG_FOR_2048:@[0-9]+]], ptr{{( addrspace\(1\))?}} [[REG_FOR_4096:@[0-9]+]]] }
// CHECK-NOT: internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ArrayHeader, [3 x ptr{{( addrspace\(1\))?}}] } { %struct.ArrayHeader { ptr{{( addrspace\(1\))?}} {{.*}}, i32 3 }, [3 x ptr{{( addrspace\(1\))?}}] [ptr{{( addrspace\(1\))?}} [[REG_FOR_1024]], ptr{{( addrspace\(1\))?}} [[REG_FOR_2048]], ptr{{( addrspace\(1\))?}} [[REG_FOR_4096]]] }

// CHECK: internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ObjHeader, ptr{{( addrspace\(1\))?}}, ptr{{( addrspace\(1\))?}}, i1 } { %struct.ObjHeader { ptr{{( addrspace\(1\))?}} {{.*}} }, ptr{{( addrspace\(1\))?}} [[REG_FOR_CLASSIFIER_FIELD:@[0-9]+]], ptr{{( addrspace\(1\))?}} [[REG_FOR_ARGUMENTS_FIELD:@[0-9]+]], i1 false }
// CHECK-NOT: internal unnamed_addr{{( addrspace\(1\))?}} constant { %struct.ObjHeader, ptr{{( addrspace\(1\))?}}, ptr{{( addrspace\(1\))?}}, i1 } { %struct.ObjHeader { ptr{{( addrspace\(1\))?}} {{.*}} }, ptr{{( addrspace\(1\))?}} [[REG_FOR_CLASSIFIER_FIELD]], ptr{{( addrspace\(1\))?}} [[REG_FOR_ARGUMENTS_FIELD]], i1 false }

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(1024)
    println(1024)

    println(arrayOf(1024, 2048, 4096))
    println(arrayOf(1024, 2048, 4096))

    println(typeOf<Map<String, String>>())
    println(typeOf<Map<String, String>>())
    return "OK"
}
