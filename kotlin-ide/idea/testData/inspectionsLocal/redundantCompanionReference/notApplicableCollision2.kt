// PROBLEM: none
package my.sample

class Outer {
    class Class {
        companion object Class {
            fun say() {}
        }
    }
}

fun test() {
    Outer.Class<caret>.say()
}
