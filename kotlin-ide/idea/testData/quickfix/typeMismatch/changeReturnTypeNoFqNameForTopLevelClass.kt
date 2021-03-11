// "Change return type of enclosing function 'A.foo' to 'Int'" "true"
package foo.bar

class A {
    fun foo(): String {
        return <caret>1
    }
}

