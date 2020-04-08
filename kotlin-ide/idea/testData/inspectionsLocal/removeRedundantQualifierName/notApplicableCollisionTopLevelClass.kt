// WITH_RUNTIME
// PROBLEM: none
package my.simple.name

open class SuperClass {
    companion object {
        fun check() {}
    }
}

class Foo

class Child : SuperClass() {
    class Foo constructor() {
        constructor(i: Int) : this()
        class SuperClass
        fun check() {}

        fun foo() {
            my.simple.name<caret>.SuperClass.check()
        }

        companion object {
            fun check() {}
        }
    }
}