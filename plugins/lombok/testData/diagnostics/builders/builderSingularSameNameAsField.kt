// RUN_PIPELINE_TILL: CODEGEN
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
    User.builder().tags(listOf("a", "b")).tags("c").build()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
