// ISSUE: KT-53122
// IGNORE_BACKEND_K1: JVM_IR
// MODULE: annotations

@Retention(AnnotationRetention.SOURCE)
annotation class NoArg

// MODULE: a(annotations)

@NoArg
abstract class Base(val x: Int)

// MODULE: b(a, annotations)

@NoArg
abstract class Derived(x: Int) : Base(x)

fun box(): String {
    return "OK"
}
