// RUN_PIPELINE_TILL: FRONTEND
typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen

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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, override, typeAliasDeclaration */
