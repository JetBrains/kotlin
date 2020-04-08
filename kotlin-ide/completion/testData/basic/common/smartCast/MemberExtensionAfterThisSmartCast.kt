class A {
    fun String.foo() {}
}

fun Any.test() { // see KT-8865 Member extension after this smart cast not working
    if (this is A) {
        "".<caret>
    }
}


// EXIST: foo