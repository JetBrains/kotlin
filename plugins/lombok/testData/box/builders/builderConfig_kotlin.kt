// FILE: main.kt
import lombok.Builder
import lombok.Data

@Builder
class User(val name: String)

fun box(): String {
    val userBuilder: User.SpecialUserBuilder = User.builder()
    return "OK"
}

// FILE: lombok.config
lombok.builder.className=Special*Builder
