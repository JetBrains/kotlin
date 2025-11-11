// RUN_PIPELINE_TILL: FRONTEND
@MyTypeAlias
class A {
    fun foo() {

    }
}

@MyTypeAlias
class B : A() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun foo() {

    }
}

typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, override, typeAliasDeclaration */
