package test

class A

class Many {
    companion object {
        val x = A()
    }

    companion object {
        val y = <caret>A()
    }
}

// REF: (test).A