// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89739

import lombok.Builder;

@Builder
class Mutable {
    var x: Int = 0

    fun test() {
        val ex = builder()
            .x(42)
            .build()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
