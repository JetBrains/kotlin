package test

class A

class Test {
    fun some(vararg a: A) {}
    fun call() = some(<caret>a = arrayOf(A()))
}

// REF: a
