// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89048
// FIR_DUMP
// WITH_STDLIB
// FULL_JDK
// DISABLE_JAVA_FACADE

// FILE: SingularConflict.java
import java.util.List;
import lombok.Builder;
import lombok.Singular;

// The Java analogue of `builderSingularSameNameAsOtherProperty_kotlin.kt`: both `oneSheep(String)` setters are generated
// Questionable case: Java code is red, should the use-site in Kotlin also be red is unclear
@Builder
public class SingularConflict {
    private String oneSheep;
    @Singular("oneSheep") private List<String> sheep;
}

// FILE: test.kt
fun test() {
    SingularConflict.builder().oneSheep("a").build()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, stringLiteral */
