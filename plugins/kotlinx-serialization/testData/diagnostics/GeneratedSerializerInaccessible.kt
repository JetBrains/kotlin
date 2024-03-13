// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*

@Serializable
class A

fun foo() {
    A.<!DEPRECATION_ERROR!>`$serializer`<!>
    A.<!DEPRECATION_ERROR!>`$serializer`<!>.descriptor
    A(<!TOO_MANY_ARGUMENTS!>0<!>, <!TOO_MANY_ARGUMENTS!>null<!>)
}
