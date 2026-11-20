// RUN_PIPELINE_TILL: FRONTEND
// The compiler matches `annotated` predicates on local classes through their resolved annotations, which see through type aliases
typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen2

@MyTypeAlias
class TopLevel

class TopLevelSub : <!FINAL_SUPERTYPE!>TopLevel<!>()

fun test() {
    @MyTypeAlias
    class Local

    class LocalSub : Local()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, typeAliasDeclaration */
