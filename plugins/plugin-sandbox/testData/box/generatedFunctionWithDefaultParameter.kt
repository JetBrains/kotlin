// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
//  ^Reason: KT-82482
// DUMP_IR
package foo

annotation class MyAnnotation

@MyAnnotation
class Foo

fun box(): String {
    val foo = Foo()
    return foo.idWithDefault()
}
