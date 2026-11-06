// ISSUE: KT-91272
// FIR_DUMP

import lombok.Data

class A {
    @Data
    companion object {
        val count: Int = 1
        val name: String = "object"
    }
}

@Data
object B {
    val x: Int = 2
    val z: Boolean = true
}

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
interface C {
    val s: String
}

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
enum class E(val y: Int = 0) {
    <!WRONG_ANNOTATION_TARGET!>@Data<!>
    FIRST,
    <!WRONG_ANNOTATION_TARGET!>@Data<!>
    SECOND(4);
}

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
annotation class Ann(val s: String)

fun foo() {
    @Data
    class A(val count: Int, val name: String)

    <!LOCAL_INTERFACE_NOT_ALLOWED!><!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
    interface C<!> {
        val s: String
    }
}
