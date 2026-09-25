// ISSUE: KT-83273
// FIR_DUMP

// FILE: Owner.kt
import lombok.Builder;

@Builder
public class Owner(val a: Int)

// FILE: test.kt
fun usage() {
    val builder = Owner.<!INVISIBLE_REFERENCE!>OwnerBuilder<!>()
    builder.a(1).build()
    val justBuilder: Owner.OwnerBuilder? = null
    justBuilder?.a(2)?.build()
}
