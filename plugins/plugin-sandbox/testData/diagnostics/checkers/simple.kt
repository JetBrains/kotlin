// RUN_PIPELINE_TILL: BACKEND
fun <!FUNCTION_WITH_DUMMY_NAME!>dummy<!>(x: Int) {

}

class A {
    fun <!FUNCTION_WITH_DUMMY_NAME!>dummy<!>() {

    }
}

class B {
    val dummy: Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
