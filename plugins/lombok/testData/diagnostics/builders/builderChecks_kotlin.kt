// WITH_STDLIB

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
