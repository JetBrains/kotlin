// "Cast expression 'Foo<Number>()' to 'Foo<Int>'" "true"
class Foo<out T>

fun foo(): Foo<Int> {
    return Foo<Number>()<caret>
}