// "Change return type of enclosing function 'Companion.foo' to 'Int'" "true"
package foo.bar

class A {
    companion object {
        fun foo(): String {
            return <caret>1
        }
    }
}

