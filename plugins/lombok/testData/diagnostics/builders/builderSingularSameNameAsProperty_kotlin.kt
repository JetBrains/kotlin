// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-91440

import lombok.Builder
import lombok.Singular

@Builder
class User(@Singular("tags") val tags: List<String>)

fun test() {
    User.builder().tags(listOf("a", "b")).tags("c").build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
