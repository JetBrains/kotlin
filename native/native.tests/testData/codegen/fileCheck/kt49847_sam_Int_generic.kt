// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun interface Foo<T> {
    fun bar(x: T): Int
}

fun baz(x: Any): Int = x.hashCode()

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
// CHECK-OPT-NOT: Int-box
// CHECK-DEBUG: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val foo: Foo<Int> = Foo(::baz)
    val result = foo.bar(42)
    return if( result == 42 )
        "OK"
    else "FAIL: $result"
}
