// ISSUE: KT-90359
// FILE: usage.kt
fun usage(): User = User.builder().name("Alice").build()

// FILE: User.kt
import lombok.Builder

@Builder
class User(val name: String) 
