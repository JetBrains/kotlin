package test

class A {
    val foo = 42

    fun getFoo() = ""

    fun test() {
        getF<caret>
    }

}

// ORDER: getFoo
// ORDER: getFaa
// ORDER: foo