// LL_FIR_DIVERGENCE
// KT-75685: LLFirIdePredicateBasedProvider collects all owners lazily and independently from the compiler required annotations phase
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// The compiler matches `annotated` predicates on local classes through their resolved annotations, which see through type aliases
typealias MyTypeAlias = org.jetbrains.kotlin.plugin.sandbox.AllOpen2

@MyTypeAlias
class TopLevel

class TopLevelSub : TopLevel()

fun test() {
    @MyTypeAlias
    class Local

    class LocalSub : Local()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, typeAliasDeclaration */
