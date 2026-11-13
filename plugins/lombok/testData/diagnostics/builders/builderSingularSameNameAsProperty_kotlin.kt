// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-91440

import lombok.Builder
import lombok.Singular

@Builder
class User(@Singular("tags") val tags: List<String>)

fun test() {
    User.builder().tags(<!ARGUMENT_TYPE_MISMATCH!>listOf("a", "b")<!>).tags("c").build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
