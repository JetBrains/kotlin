// CORRECT_ERROR_TYPES
// EXPECTED_ERROR: (kotlin:10:1) cannot find symbol


@file:Suppress("UNRESOLVED_REFERENCE")
import com.example.Unresolved1
import org.example.*
import org.another.*

@Unresolved1
@Unresolved2
class C