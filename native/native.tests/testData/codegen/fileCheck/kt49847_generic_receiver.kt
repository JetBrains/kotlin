// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun <T> T.foo() { println(this) }

// CHECK-LABEL: define void @"kfun:#bar(0:0){0\C2\A7<kotlin.Any?>}"
// CHECK-EAGER_SHADOW_STACK-SAME: (ptr [[x:%[0-9]+]])
// CHECK-LATE_SHADOW_STACK-SAME: (ptr addrspace(1) [[x:%[0-9]+]])
fun <BarTP> bar(x: BarTP) {
    // CHECK-OPT-EAGER_SHADOW_STACK: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr {{%[0-9]+}}, ptr [[x]])
    // CHECK-OPT-LATE_SHADOW_STACK: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr addrspace(1) {{%[0-9]+}}, ptr addrspace(1) [[x]])
    // CHECK-DEBUG-EAGER_SHADOW_STACK: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr {{%[0-9]+}}, ptr {{%[0-9]+}})
    // CHECK-DEBUG-LATE_SHADOW_STACK: call void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"(ptr addrspace(1) {{%[0-9]+}}, ptr addrspace(1) {{%[0-9]+}})
    println(x::foo)
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-EAGER_SHADOW_STACK: call void @"kfun:box$$FUNCTION_REFERENCE_FOR$foo$1.<init>#internal"(ptr {{%[0-9]+}}, i32 5)
    // CHECK-LATE_SHADOW_STACK: call void @"kfun:box$$FUNCTION_REFERENCE_FOR$foo$1.<init>#internal"(ptr addrspace(1) {{%[0-9]+}}, i32 5)
    println(5::foo)

    bar("hello")
    bar(42)
    return "OK"
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define internal void @"kfun:bar$$FUNCTION_REFERENCE_FOR$foo$0.<init>#internal"
// CHECK-EAGER_SHADOW_STACK-SAME: (ptr {{%[0-9]+}}, ptr {{%[0-9]+}})
// CHECK-LATE_SHADOW_STACK-SAME: (ptr addrspace(1) {{%[0-9]+}}, ptr addrspace(1) {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:box$$FUNCTION_REFERENCE_FOR$foo$1.<init>#internal"
// CHECK-EAGER_SHADOW_STACK-SAME: (ptr {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK-LATE_SHADOW_STACK-SAME: (ptr addrspace(1) {{%[0-9]+}}, i32 {{%[0-9]+}})
