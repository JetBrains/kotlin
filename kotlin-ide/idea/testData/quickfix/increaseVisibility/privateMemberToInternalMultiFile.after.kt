// "Make 'foo' internal" "true"
// ACTION: Make 'foo' public
// ERROR: Cannot access 'foo': it is private in 'First'

package test

class Second(val f: First) {
    fun bar() = f.<caret>foo()
}
