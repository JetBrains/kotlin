// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun <T> T.foo() { println(this) }

// CHECK-LABEL: define void @"kfun:#bar(0:0){0\C2\A7<kotlin.Any?>}"
// CHECK-SAME: (%struct.ObjHeader* [[x:%[0-9]+]])
fun <BarTP> bar(x: BarTP) {
    // CHECK-OPT: call void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* [[x]])
    // CHECK-DEBUG: call void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"(%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})
    println(x::foo)
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK: call void @"kfun:$foo$FUNCTION_REFERENCE$1.<init>#internal"(%struct.ObjHeader* {{%[0-9]+}}, i32 5)
    println(5::foo)

    bar("hello")
    bar(42)
    return "OK"
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$0.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, %struct.ObjHeader* {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:$foo$FUNCTION_REFERENCE$1.<init>#internal"
// CHECK-SAME: (%struct.ObjHeader* {{%[0-9]+}}, i32 {{%[0-9]+}})
