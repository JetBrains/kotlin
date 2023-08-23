/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.reflect.*

// CHECK: [[REG_FOR_1024:@[0-9]+]] = internal unnamed_addr constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { %struct.TypeInfo* {{.*}} }, i32 1024 }
// CHECK-NOT: internal unnamed_addr constant { %struct.ObjHeader, i32 } { %struct.ObjHeader { %struct.TypeInfo* {{.*}} }, i32 1024 }

// CHECK: internal unnamed_addr constant { %struct.ArrayHeader, [3 x %struct.ObjHeader*] } { %struct.ArrayHeader { %struct.TypeInfo* {{.*}}, i32 3 }, [3 x %struct.ObjHeader*] [%struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_1024]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_2048:@[0-9]+]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_4096:@[0-9]+]], i32 0, i32 0)] }
// CHECK-NOT: internal unnamed_addr constant { %struct.ArrayHeader, [3 x %struct.ObjHeader*] } { %struct.ArrayHeader { %struct.TypeInfo* {{.*}}, i32 3 }, [3 x %struct.ObjHeader*] [%struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_1024]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_2048:@[0-9]+]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i32 }, { %struct.ObjHeader, i32 }* [[REG_FOR_4096:@[0-9]+]], i32 0, i32 0)] }

// CHECK: internal unnamed_addr constant { %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader*, i1 } { %struct.ObjHeader { %struct.TypeInfo* {{.*}} }, %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i8* }, { %struct.ObjHeader, i8* }* [[REG_FOR_CLASSIFIER_FIELD:@[0-9]+]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader* }, { %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader* }* [[REG_FOR_ARGUMENTS_FIELD:@[0-9]+]], i32 0, i32 0), i1 false }
// CHECK-NOT: internal unnamed_addr constant { %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader*, i1 } { %struct.ObjHeader { %struct.TypeInfo* {{.*}} }, %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, i8* }, { %struct.ObjHeader, i8* }* [[REG_FOR_CLASSIFIER_FIELD:@[0-9]+]], i32 0, i32 0), %struct.ObjHeader* getelementptr inbounds ({ %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader* }, { %struct.ObjHeader, %struct.ObjHeader*, %struct.ObjHeader* }* [[REG_FOR_ARGUMENTS_FIELD:@[0-9]+]], i32 0, i32 0), i1 false }

fun main() {
    println(1024)
    println(1024)

    println(arrayOf(1024, 2048, 4096))
    println(arrayOf(1024, 2048, 4096))

    println(typeOf<Map<String, String>>())
    println(typeOf<Map<String, String>>())
}