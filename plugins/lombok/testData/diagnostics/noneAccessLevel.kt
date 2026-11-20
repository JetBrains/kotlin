// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85693

// FILE: NoneAccessLevel.java

import lombok.*;

@AllArgsConstructor(access = AccessLevel.NONE)
public class NoneAccessLevel {
    int intField;
}

// FILE: test.kt

fun usage() {
    NoneAccessLevel(<!TOO_MANY_ARGUMENTS!>1<!>);
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType */
