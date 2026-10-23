// ISSUE: KT-90359
// FILE: Usages.kt

fun builderUsages() {
    val userBuilder: User.<!UNRESOLVED_REFERENCE!>UserBuilder<!> = User.<!UNRESOLVED_REFERENCE!>builder<!>()
    val user: User = userBuilder.<!UNRESOLVED_REFERENCE!>name<!>("Alice").age(30).email(null).build()
}

// FILE: User.kt

import lombok.AccessLevel
import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder(toBuilder = true)<!>
class User(val name: String, val age: Int, val email: String?)
