// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89048
// FIR_DUMP
// WITH_STDLIB
// FULL_JDK

// FILE: SingularConflict.java
import java.util.List;
import lombok.Builder;
import lombok.Singular;

// The Java analogue of `builderSingularSameNameAsOtherProperty_kotlin.kt`: both `oneSheep(String)` setters are generated
// Questionable case as Java code is red. Currently decided that Kotlin use-site should be red too.
@Builder
public class SingularConflict {
    private String oneSheep;
    @Singular("oneSheep") private List<String> sheep;
}

// FILE: test.kt
fun test() {
    SingularConflict.builder().<!OVERLOAD_RESOLUTION_AMBIGUITY!>oneSheep<!>("a").<!UNRESOLVED_REFERENCE!>build<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, stringLiteral */
