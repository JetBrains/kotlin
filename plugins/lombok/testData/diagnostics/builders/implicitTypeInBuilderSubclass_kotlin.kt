// WITH_STDLIB
// FILE: CustomBuilders.kt

// The status of `build` is resolved against the builder's members, and the builder's `name` field and setter take
// their type from `User.name`, whose implicit type is not resolved yet at the STATUS phase
class CustomUserBuilder : User.UserBuilder() {
    override fun build(): User = super.build()
}

class CustomMixedBuilder : Mixed.MixedBuilder() {
    override fun build(): Mixed = super.build()
}

fun bob(): User = CustomUserBuilder().name("Bob").build()

fun mixed(): Mixed = CustomMixedBuilder().id(42).title("Main").owner(bob()).build()

// FILE: ToBuild.kt

import lombok.Builder

@Builder
class User {
    var name = "Alice"
}

@Builder
class Mixed {
    var id: Int = 0
    var title: String = ""
    var stuff = emptyList<String>()
    var owner: User = User()
    var initialized = false
}
