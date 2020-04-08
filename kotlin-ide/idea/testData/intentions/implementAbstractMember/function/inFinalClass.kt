// IS_APPLICABLE: false
// ERROR: Abstract function 'foo' in non-abstract class 'A'
class A {
    abstract fun <caret>foo(): Int
}