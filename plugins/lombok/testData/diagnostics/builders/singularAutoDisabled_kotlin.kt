// WITH_STDLIB

// FILE: test.kt

import lombok.Builder
import lombok.Singular

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class Basket(
    @Singular
    val items: List<String>,
)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class BasketWithExplicitName(
    @Singular("fruit")
    val fruits: List<String>,
)

// FILE: lombok.config

lombok.singular.auto=false
