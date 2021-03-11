// "Add remaining branches" "true"
// WITH_RUNTIME

sealed class FooSealed
object A: FooSealed()
object B: FooSealed()
object `C`: FooSealed()
class D: FooSealed()
class `true`: FooSealed()
class `false`: FooSealed()
object `null`: FooSealed()

fun test(foo: FooSealed?) = <caret>when (foo) {
    A -> "A"
}