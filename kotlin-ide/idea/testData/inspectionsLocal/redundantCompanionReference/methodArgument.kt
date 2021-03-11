// PROBLEM: none

class C {
    companion object {
    }
}

class Test {
    fun foo(i: Any) {
    }

    fun test() {
        this.foo(<caret>C)
    }
}