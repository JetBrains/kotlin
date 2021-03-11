// IS_APPLICABLE: false

interface Foo

fun test() {
    val value = object : Foo {}<caret>
}