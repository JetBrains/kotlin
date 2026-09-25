// ISSUE: KT-83329
// FIR_DUMP
// FILE: Owner.kt
import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
class Owner(val a: Int) {
    companion object {
        fun builder(a: Int): OwnerBuilder = internalBuilder().a(1)
    }
}


// FILE: test.kt
fun foo() {
    val internalBuilder = Owner.internalBuilder()
    val builder = Owner.builder(1)
}
