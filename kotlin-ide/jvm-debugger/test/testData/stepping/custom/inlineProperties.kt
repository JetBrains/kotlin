package inlineProperties

fun main(args: Array<String>) {
    class A {
        inline val a: String
            get() {
                //Breakpoint!
                return System.nanoTime().toString()
            }
    }

    A().apply { a }
    B().apply { b }
}

class B {
    inline val b: String
        get() {
            //Breakpoint!
            return System.nanoTime().toString()
        }
}

// RESUME: 2