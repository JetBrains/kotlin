package t

class A {
    companion object Companion {
        fun test()
    }
}

fun test() {
    <caret>A.test()
}


// REF: companion object of (t).A

