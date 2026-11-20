// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90359
// FILE: usage.kt
fun usage(): User = User.builder().name("Alice").build()

// FILE: User.kt
import lombok.Builder

@Builder
class User(val name: String)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
