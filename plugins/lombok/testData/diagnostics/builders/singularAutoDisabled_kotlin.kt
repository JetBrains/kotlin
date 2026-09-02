// WITH_STDLIB

// FILE: test.kt

import lombok.Builder
import lombok.Singular

@Builder
class Basket(
    <!SINGULAR_REQUIRES_EXPLICIT_NAME!>@Singular<!>
    val items: List<String>,
)

@Builder
class BasketWithExplicitName(
    @Singular("fruit")
    val fruits: List<String>,
)

// FILE: lombok.config

lombok.singular.auto=false
