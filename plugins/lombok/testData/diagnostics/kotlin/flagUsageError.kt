// WITH_STDLIB
// FULL_JDK
// WITH_ADVANCED_LOGGERS

// FILE: test.kt

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.extern.apachecommons.CommonsLog
import lombok.extern.flogger.Flogger
import lombok.extern.jbosslog.JBossLog
import lombok.extern.log4j.Log4j2
import lombok.extern.slf4j.XSlf4j
import lombok.ToString
import lombok.EqualsAndHashCode

// Error despite the `lombok.log.javaUtilLogging.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Slf4j<!>
class LogExampleError

// Error despite the `lombok.log.slf4j.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Slf4j<!>
class Slf4jExampleError

// Error despite the `lombok.log.log4j.flagUsage=warning` because `lombok.log.flagUsage=error` has greater severity
<!FLAG_USAGE_ERROR!>@Log4j<!>
class Log4jExampleError

// Error despite the `lombok.log.apacheCommons.flagUsage=warning` because `error` has greater severity
<!FLAG_USAGE_ERROR!>@CommonsLog<!>
class CommonsLogExampleError

// Error despite the `lombok.log.flogger.flagUsage=warning` because `error` has greater severity
<!FLAG_USAGE_ERROR!>@Flogger<!>
class FloggerExampleError

// Error despite the `lombok.log.jbossLog.flagUsage=warning` because `error` has greater severity
<!FLAG_USAGE_ERROR!>@JBossLog<!>
class JBossLogExampleError

// Error despite the `lombok.log.log4j2.flagUsage=warning` because `error` has greater severity
<!FLAG_USAGE_ERROR!>@Log4j2<!>
class Log4j2ExampleError

// Error despite the `lombok.log.xslf4j.flagUsage=warning` because `error` has greater severity
<!FLAG_USAGE_ERROR!>@XSlf4j<!>
class XSlf4jExampleError

<!FLAG_USAGE_ERROR!>@ToString<!>
class ToStringExampleError(val x: Int)

<!FLAG_USAGE_ERROR!>@EqualsAndHashCode<!>
class EqualsAndHashCodeExampleError(val x: Int)

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=error
lombok.log.javaUtilLogging.flagUsage=warning
lombok.log.slf4j.flagUsage=warning
lombok.log.log4j.flagUsage=warning
lombok.log.apacheCommons.flagUsage=warning
lombok.log.flogger.flagUsage=warning
lombok.log.jbossLog.flagUsage=warning
lombok.log.log4j2.flagUsage=warning
lombok.log.xslf4j.flagUsage=warning
lombok.toString.flagUsage=error
lombok.equalsAndHashCode.flagUsage=error
