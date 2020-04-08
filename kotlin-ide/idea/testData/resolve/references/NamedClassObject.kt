package a

class A {
    companion object Named {
        val i: Int
    }
}

fun main(args: Array<String>) {
    A.Na<caret>med.i
}

// REF: companion object of (a).A