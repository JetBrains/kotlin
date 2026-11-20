// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-91440
// FILE: User.java
import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public class User {
    @Singular("tags") private List<String> tags;
}

// FILE: test.kt
fun test() {
    User.builder().tags(<!ARGUMENT_TYPE_MISMATCH!>listOf("a", "b")<!>).tags("c").build()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
