// CORRECT_ERROR_TYPES
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ `@file:Suppress("UNRESOLVED_REFERENCE")` causes the visibility checker to crash when checking annotation visibility

@file:Suppress("UNRESOLVED_REFERENCE")
import com.example.Unresolved1
import org.example.*
import org.another.*

@Unresolved1
@Unresolved2
class C
