// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-90709

// FILE: test.kt

import lombok.Builder
import lombok.Singular

// A regular property is a builder field just like a constructor parameter, so `@Singular` on it should be checked the same way.
@Builder
class SingularChecksOnParameters(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val numbers: IntArray,
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!> val stuff: List<String>,
    @Singular("item") <!BUILDER_DEFAULT_AND_SINGULAR_MIXED!>@Builder.Default<!> val items: List<String> = emptyList(),
)

@Builder
class SingularChecksOnProperties {
    // Should be UNSUPPORTED_SINGULAR_TYPE, as for a parameter
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> var numbers: IntArray = IntArray(0)
    // Should be CANNOT_SINGULARIZE_NAME, as for a parameter
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!> var stuff: List<String> = emptyList()
    // Should be BUILDER_DEFAULT_AND_SINGULAR_MIXED, as for a parameter
    @Singular("item") <!BUILDER_DEFAULT_AND_SINGULAR_MIXED!>@Builder.Default<!> var items: List<String> = emptyList()
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */
