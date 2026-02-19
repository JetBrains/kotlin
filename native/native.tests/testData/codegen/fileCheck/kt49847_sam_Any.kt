// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun interface Foo {
    fun bar(x: Int): Any
}

fun baz(x: Any): Int = x.hashCode()

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:

fun box(): String {
    val foo: Foo = Foo(::baz)
    return if (foo.bar(42) == 42)
        "OK"
    else "FAIL"
}
