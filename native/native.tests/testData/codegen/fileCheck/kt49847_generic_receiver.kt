// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun <T> T.foo() { println(this) }

// CHECK-LABEL: define void @"kfun:#bar(0:0){0\C2\A7<kotlin.Any?>}"
// CHECK-SAME: (ptr [[x:%[0-9]+]])
fun <BarTP> bar(x: BarTP) {
    // CHECK-OPT: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr {{%[0-9]+}}, ptr [[x]])
    // CHECK-DEBUG: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr {{%[0-9]+}}, ptr {{%[0-9]+}})
    println(x::foo)
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK: call void @"kfun:box$$FUNCTION_REFERENCE_FOR$foo$1.<init>#internal"(ptr {{%[0-9]+}}, i32 5)
    println(5::foo)

    bar("hello")
    bar(42)
    return "OK"
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define internal void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"
// CHECK-SAME: (ptr {{%[0-9]+}}, ptr {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:box$$FUNCTION_REFERENCE_FOR$foo$1.<init>#internal"
// CHECK-SAME: (ptr {{%[0-9]+}}, i32 {{%[0-9]+}})
