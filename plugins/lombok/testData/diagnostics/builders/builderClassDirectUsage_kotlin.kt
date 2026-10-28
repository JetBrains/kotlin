// ISSUE: KT-83273
// FIR_DUMP

// FILE: Owner.kt
import lombok.Builder;

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
public class Owner(val a: Int)

// FILE: test.kt
fun usage() {
    val builder = Owner.<!UNRESOLVED_REFERENCE!>OwnerBuilder<!>()
    builder.a(1).build()
    val justBuilder: Owner.<!UNRESOLVED_REFERENCE!>OwnerBuilder<!>? = null
    justBuilder?.<!UNRESOLVED_REFERENCE!>a<!>(2)?.build()
}
