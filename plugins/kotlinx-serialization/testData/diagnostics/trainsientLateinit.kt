// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-73961

class Bar {
    @kotlinx.serialization.Transient
    lateinit var foo: String

    constructor(foo: String) {
        this.foo = foo
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, lateinit, propertyDeclaration, secondaryConstructor, thisExpression */
