// WITH_STDLIB

import lombok.AccessLevel
import lombok.Builder
import lombok.Singular

@Builder
class WithPlainInitializer(
    val name: String = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>"default"<!>,
)

@Builder
class WithMissingDefaultInitializer(
    <!BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION!>@Builder.Default<!>
    val name: String,
)

@Builder
class Farm(
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!>
    val sheep: List<String>,
)

@Builder
class Container(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("thing")<!>
    val things: Array<String>,
)

@Builder
class MixedDefaultAndSingular(
    <!BUILDER_DEFAULT_AND_SINGULAR_MIXED!>@Builder.Default<!>
    @Singular
    val items: List<String> = emptyList(),
)

// No diagnostics expected: builder-eligible properties used correctly.
@Builder
class CleanWidget(
    val id: Int,
    @Builder.Default
    val name: String = "default",
    @Singular
    val tags: List<String>,
)

// `@Builder` on a secondary constructor: only `@Singular` is checkable (`@Builder.Default`
// is `@Target(FIELD)`, so it can't land on a bare constructor parameter at all).
class ConstructorSingularCannotSingularize(val id: Int) {
    @Builder
    constructor(id: Int, <!CANNOT_SINGULARIZE_NAME!>@Singular<!> sheep: List<String>) : this(id)
}

class ConstructorParameterDefaultIgnored(val id: Int, val extra: Int) {
    @Builder
    constructor(id: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>0<!>) : this(id, -1)
}

@Builder(access = AccessLevel.PROTECTED)
class BuilderAccessLevelProtected(val id: Int)

@Builder(access = AccessLevel.PACKAGE) // TODO: it should be prohibited, KT-88337
class BuilderAccessLevelPackage(val id: Int)

@Builder(access = AccessLevel.<!DEPRECATION!>MODULE<!>) // TODO: it should be prohibited, KT-88337
class BuilderAccessLevelModule(val id: Int)

fun testAccessLevels() {
    BuilderAccessLevelProtected.builder() // OK, but INVISIBLE_REFERENCE is expected (KT-88337, KT-88203)
}
