// ISSUE: KT-90359
// FILE: Usages.kt

fun builderUsages() {
    val userBuilder: User.UserBuilder = User.builder()
    val user: User = userBuilder.name("Alice").age(30).email(null).build()
}

// FILE: User.kt

import lombok.AccessLevel
import lombok.Builder

@Builder(toBuilder = true)
class User(val name: String, val age: Int, val email: String?)
