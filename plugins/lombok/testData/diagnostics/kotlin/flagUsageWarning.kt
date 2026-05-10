// FULL_JDK

// FILE: test.kt

import lombok.extern.java.Log
import lombok.ToString

<!FLAG_USAGE_WARNING!>@Log<!>
class LogExampleWarning

<!FLAG_USAGE_WARNING!>@ToString<!>
class ToStringExampleWarning(val x: Int)

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=warning
lombok.toString.flagUsage=warning
