// FULL_JDK

// FILE: test.kt

import lombok.extern.java.Log
import lombok.ToString

<!FLAG_USAGE_ERROR!>@Log<!>
class LogExampleError

<!FLAG_USAGE_ERROR!>@ToString<!>
class ToStringExampleError(val x: Int)

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=error
lombok.toString.flagUsage=error
