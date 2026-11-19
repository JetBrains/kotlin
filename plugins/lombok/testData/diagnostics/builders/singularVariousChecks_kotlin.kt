// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89739
// WITH_STDLIB

import lombok.Builder
import lombok.Singular

// `@Builder` on the primary constructor: both parameters are builder fields, promoted or not.
class PrimaryConstructorBuilder @Builder constructor(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val promoted: IntArray,
    // Should be UNSUPPORTED_SINGULAR_TYPE, as for a class-level `@Builder` (lost in the checker split)
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("other")<!> notPromoted: IntArray,
)

// The same for a class-level `@Builder`.
@Builder
class ClassLevelBuilder(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val promoted: IntArray,
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("other")<!> notPromoted: IntArray,
)

// No builder at all: `@Singular` here has nothing to do with a builder.
// Should be no UNSUPPORTED_SINGULAR_TYPE (reported since the checker split)
class NoBuilder(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val promoted: IntArray) {
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("other")<!> var body: IntArray = IntArray(0)
}

// A builder on a secondary constructor only: the primary constructor parameter is not a builder field.
// Should be no UNSUPPORTED_SINGULAR_TYPE on `promoted` (reported since the checker split)
class SecondaryConstructorBuilder(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val promoted: IntArray) {
    @Builder
    constructor(<!UNSUPPORTED_SINGULAR_TYPE!>@Singular("other")<!> others: IntArray, flag: Boolean) : this(others)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor,
stringLiteral */
