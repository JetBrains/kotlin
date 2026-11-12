// RUN_PIPELINE_TILL: CODEGEN
// FILE: usage.kt

fun usage(): User = User.builder().name("Alice").build()

// FILE: User.kt

import lombok.Builder

abstract class Base {
    abstract fun foo()
}

@Builder
class User {
    var name: String = ""

    class UserBuilder : Base() {
        override fun foo() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, override, propertyDeclaration, stringLiteral */
