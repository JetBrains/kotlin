// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89048
// FIR_DUMP
// WITH_STDLIB

import lombok.Builder

// Both constructor builders populate the same `SharedBuilder` class (the default builder class name), so `x(String)`
// is one setter shared by both of them, as in Lombok, which skips a builder method that already exists.
// The code should be green
class Shared {
    @Builder(builderMethodName = "first")
    constructor(x: String)

    @Builder(builderMethodName = "second")
    constructor(x: String, y: Int)
}

// The same with suppressed builder factories.
class SharedWithoutFactory {
    @Builder(builderMethodName = "")
    constructor(x: String)

    @Builder(builderMethodName = "")
    constructor(x: String, y: Int)
}

// The same builder method name: here only one `x(String)` is generated.
class SharedSameFactory {
    @Builder
    constructor(x: String)

    @Builder
    constructor(x: String, y: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, secondaryConstructor, stringLiteral */
