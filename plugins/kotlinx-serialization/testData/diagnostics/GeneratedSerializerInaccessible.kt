// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
class A

fun foo() {
    A.<!UNRESOLVED_REFERENCE!>`$serializer`<!>
    A.<!UNRESOLVED_REFERENCE!>`$serializer`<!>.<!UNRESOLVED_REFERENCE!>descriptor<!>
    A(<!TOO_MANY_ARGUMENTS!>0<!>, <!TOO_MANY_ARGUMENTS!>null<!>)
}
