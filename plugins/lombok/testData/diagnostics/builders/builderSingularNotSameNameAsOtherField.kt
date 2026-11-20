// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89048
// FIR_DUMP
// WITH_STDLIB
// FULL_JDK

// FILE: SingularConflict.java
import java.util.List;
import lombok.Builder;
import lombok.Singular;

// See also builderSingularSameNameAsOtherField.kt
@Builder
public class SingularConflict {
    private String oneSheep;
    @Singular("oneSheep") private List<Integer> sheep;
}

// FILE: test.kt
fun test() {
    SingularConflict.builder().oneSheep("a").oneSheep(2).build()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, stringLiteral */
