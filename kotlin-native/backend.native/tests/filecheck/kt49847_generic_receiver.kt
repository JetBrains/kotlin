/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun <T> T.foo() { println(this) }

// CHECK-LABEL: define void @"kfun:#bar(0:0){0\C2\A7<kotlin.Any?>}"
// CHECK-SAME: (%struct.ObjHeader* [[x:%[0-9]+]])
fun <BarTP> bar(x: BarTP) {
    // CHECK: call void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* [[x]])
    println(x::foo)
}

// CHECK-LABEL: define void @"kfun:#main(){}"
fun main() {
    // CHECK: call void @"kfun:$foo$FUNCTION_REFERENCE$1.<init>#internal"(%struct.ObjHeader* {{%[0-9]+}}, i32 5)
    println(5::foo)

    bar("hello")
    bar(42)
}

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$1.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}})
