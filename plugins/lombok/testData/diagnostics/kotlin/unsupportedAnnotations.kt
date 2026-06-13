
// FILE: KotlinClass.kt

import lombok.*

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!> // Not yet supported
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!> // Isn't going to be supported
class KotlinClass {
    companion object
}

// Make sure compiler doesn't crash on unsupported annotations that are being processed

<!ANNOTATION_IS_NOT_SUPPORTED!>@AllArgsConstructor<!>
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!>
class ConstructorExample<A, B>(val a: A, val b: B, val C: String)


// FILE: test.kt

fun test() {
    // Currently "Kotlin compiler ignores Lombok annotations if you use them in Kotlin code."
    val builder: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.<!UNRESOLVED_REFERENCE!>builder<!>()
    val builder2: <!UNRESOLVED_REFERENCE!>KotlinClassBuilder<!> = KotlinClass.Companion.<!UNRESOLVED_REFERENCE!>builder<!>()
}
