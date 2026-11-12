// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89739, KT-90508

import lombok.Builder;

@Builder
class Mutable {
    private var x: Int = 0

    private var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>y<!> = 1

    fun test() {
        val ex = builder()
            .x(42)
            .build()

        val ex2 = builder()
            .<!UNRESOLVED_REFERENCE!>y<!>(13)
            .build()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
