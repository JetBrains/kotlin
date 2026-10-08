// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun interface Foo<T> {
    fun bar(x: T): Any
}

fun baz(x: Any): Int = x.hashCode()

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val foo: Foo<Int> = Foo(::baz)
    return if (foo.bar(42) == 42)
        "OK"
    else "FAIL"
}
