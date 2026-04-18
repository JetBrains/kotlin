@file:ToString

import lombok.ToString

@ToString
interface Interface

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
typealias TA = String

val toStringOnAnonymousObject = @ToString object {}
