
// FILE: KotlinClass.kt

import lombok.Builder
import lombok.extern.slf4j.Slf4j
import lombok.RequiredArgsConstructor

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!> // Not yet supported
<!ANNOTATION_IS_NOT_SUPPORTED!>@Slf4j<!> // Not yet supported
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!> // Isn't going to be supported
class KotlinClass {
    companion object
}

// FILE: test.kt

fun test() {
    // Currently "Kotlin compiler ignores Lombok annotations if you use them in Kotlin code."
    val builder: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.<!UNRESOLVED_REFERENCE!>builder<!>()
    val builder2: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.Companion.<!UNRESOLVED_REFERENCE!>builder<!>()
}
