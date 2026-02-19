// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class NoArg

class Outer {
    @NoArg
    inner class <!NOARG_ON_INNER_CLASS_ERROR!>Inner<!>(val b: Any)
}

fun local() {
    @NoArg
    class <!NOARG_ON_LOCAL_CLASS_ERROR!>Local<!>(val l: Any) {
        @NoArg
        inner class <!NOARG_ON_INNER_CLASS_ERROR!>InnerLocal<!>(val x: Any)
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, inner, localClass,
primaryConstructor, propertyDeclaration */
