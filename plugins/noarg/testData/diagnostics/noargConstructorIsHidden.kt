// RUN_PIPELINE_TILL: FRONTEND
annotation class NoArg

@NoArg
class MyClass(val s: String)

fun usage() {
    <!NO_VALUE_FOR_PARAMETER!>MyClass<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, primaryConstructor,
propertyDeclaration */
