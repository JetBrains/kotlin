// WITH_RUNTIME
package my.simple.name

class Outer {
    fun goo(i: Outer<caret>.Middle.Inner.Int) {

    }
    class Middle {
        class Inner {
            class Int
        }
    }
}
