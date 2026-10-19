// FILE: usage.kt

fun usage(): User = User.builder().name("Alice").build()

// FILE: User.kt

import lombok.Builder

abstract class Base {
    abstract fun foo()
}

@Builder
class User {
    var name: String = ""

    class UserBuilder : Base() {
        override fun foo() {}
    }
}
