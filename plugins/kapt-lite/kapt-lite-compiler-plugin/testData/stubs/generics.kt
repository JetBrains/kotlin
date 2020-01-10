package generics

class CIn<in T>
class COut<out T>
class C<T>

fun x(a: CIn<CharSequence>, b: COut<CharSequence>, c: C<CharSequence>) {}
fun y(a: C<in CharSequence>, b: C<out CharSequence>, c: C<CharSequence>, d: C<*>) {}

class Foo<T> {
    inner class Bar<U> {
        fun foo(t: T, u: U) {}
    }
}

fun <T : CharSequence> T.ext(x: Int) {}

fun <Z> List<Z>.ext2() {}

fun List<*>.ext3() {}

val <MyType : Runnable> MyType.extProp: Int
    get() = 0