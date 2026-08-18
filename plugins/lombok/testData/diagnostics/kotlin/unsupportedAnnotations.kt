
// FILE: KotlinClass.kt

import lombok.*
import lombok.experimental.*;

<!ANNOTATION_IS_NOT_SUPPORTED!>@SuperBuilder<!> // Not yet supported
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!> // Isn't going to be supported
class KotlinClass {
    companion object
}

// Make sure compiler doesn't crash on unsupported annotations that are being processed

<!ANNOTATION_IS_NOT_SUPPORTED!>@AllArgsConstructor<!>
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!>
class ConstructorExample<A, B>(val a: A, val b: B, val C: String)
