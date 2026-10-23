// ISSUE: KT-90359
// FILE: usage.kt
fun usage(): User = User.<!UNRESOLVED_REFERENCE!>builder<!>().name("Alice").build()

// FILE: User.kt
import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder<!>
class User(val name: String) 
