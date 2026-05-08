// FULL_JDK

// FILE: test.kt

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.ToString

<!FLAG_USAGE_ERROR!>@ToString<!>
class ToStringExampleError(val x: Int)

// Error despite the `lombok.log.javaUtilLogging.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Slf4j<!>
class LogExampleError

// Error despite the `lombok.log.slf4j.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Slf4j<!>
class Slf4jExampleError

// Error despite the `lombok.log.log4j.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Log4j<!>
class Log4jExampleError

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=error
lombok.log.javaUtilLogging.flagUsage=warning
lombok.log.slf4j.flagUsage=warning
lombok.log.log4j.flagUsage=warning
lombok.toString.flagUsage=error
