// FULL_JDK

// FILE: test.kt

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.ToString

<!FLAG_USAGE_WARNING!>@ToString<!>
class ToStringExampleWarning(val x: Int)

<!FLAG_USAGE_WARNING!>@Log<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class LogExampleWarning

<!FLAG_USAGE_WARNING!>@Slf4j<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class Slf4jExampleWarning

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=warning
lombok.toString.flagUsage=warning
