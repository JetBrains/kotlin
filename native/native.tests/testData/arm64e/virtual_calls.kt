// Vtable and itable entries, and the reverse adapters the runtime puts into them for Objective-C and Swift subclasses,
// are signed as C function pointers: with the IA key and no discriminator.

// CHECK-DAG: @"kclass:Base" = {{.*}}ptr ptrauth (ptr @"kfun:Base#foo(){}kotlin.Int-impl", i32 0)
// CHECK-DAG: ptr ptrauth (ptr @"kotlin2objc_kfun:Base#foo(){}kotlin.Int", i32 0) }
// A missing implementation stays null.
// CHECK-DAG: %struct.KotlinToObjCMethodAdapter { ptr @{{[0-9]+}}, i32 {{-?[0-9]+}}, i32 {{-?[0-9]+}}, i32 {{-?[0-9]+}}, i32 {{-?[0-9]+}}, ptr null }
open class Base {
    open fun foo(): Int = 1
    open fun covariant(): Any = 1
}

// CHECK-DAG: @"kclass:Derived" = {{.*}}ptr ptrauth (ptr @"kfun:Derived#covariant(){}kotlin.String", i32 0), ptr ptrauth (ptr @"kfun:Derived#foo(){}kotlin.Int", i32 0)
class Derived : Base() {
    override fun foo(): Int = 2
    override fun covariant(): String = "2"
}

open class GenericBase<T> {
    open fun take(value: T) {}
}

// CHECK-DAG: @"kclass:IntTaker" = {{.*}}ptr ptrauth (ptr @"kfun:IntTaker#$<bridge-NNU>take(kotlin.Int){}(kotlin.Any?){}", i32 0)
class IntTaker : GenericBase<Int>() {
    override fun take(value: Int) {}
}

interface I {
    fun bar(): Int
}

interface SubI : I

// CHECK-DAG: @"kifacevtable:Impl_{{-?[0-9]+}}" = internal constant [1 x ptr] [ptr ptrauth (ptr @"kfun:Impl#bar(){}kotlin.Int", i32 0)]
// So are the interface tables of synthetic classes, such as the Kotlin functions that wrap blocks from Objective-C.
// CHECK-DAG: = internal global [1 x ptr] [ptr ptrauth (ptr @invokeFunction0, i32 0)]
class Impl : SubI {
    override fun bar(): Int = 3
}

fun callFoo(base: Base) = base.foo()
fun callBar(i: I) = i.bar()

// The trampolines that call through vtables and itables authenticate the entries.
// CHECK-LABEL: define i32 @"kfun:Base#foo(){}kotlin.Int"(ptr %0)
// CHECK: call i32 %{{[0-9]+}}(ptr %0) [ "ptrauth"(i32 0, i64 0) ]
// CHECK-LABEL: define i32 @"kfun:I#bar(){}kotlin.Int"(ptr %0)
// CHECK: call i32 %{{[0-9]+}}(ptr %0) [ "ptrauth"(i32 0, i64 0) ]
// CHECK-LABEL: define i32 @"kfun:SubI#bar(){}kotlin.Int"(ptr %0)
// CHECK: call i32 %{{[0-9]+}}(ptr %0) [ "ptrauth"(i32 0, i64 0) ]
