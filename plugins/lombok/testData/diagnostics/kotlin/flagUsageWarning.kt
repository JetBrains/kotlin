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
import lombok.Builder
import lombok.experimental.SuperBuilder

<!FLAG_USAGE_WARNING!>@Log<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class LogExampleWarning

<!FLAG_USAGE_WARNING!>@Slf4j<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class Slf4jExampleWarning

<!FLAG_USAGE_WARNING!>@Log4j<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class Log4jExampleWarning

<!FLAG_USAGE_WARNING!>@CommonsLog<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class CommonsLogExampleWarning

<!FLAG_USAGE_WARNING!>@Flogger<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class FloggerExampleWarning

<!FLAG_USAGE_WARNING!>@JBossLog<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class JBossLogExampleWarning

<!FLAG_USAGE_WARNING!>@Log4j2<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class Log4j2ExampleWarning

<!FLAG_USAGE_WARNING!>@XSlf4j<!> // Warning because `lombok.log.flagUsage` is applicable for all log annotations
class XSlf4jExampleWarning

<!FLAG_USAGE_WARNING!>@ToString<!>
class ToStringExampleWarning(val x: Int)

<!FLAG_USAGE_WARNING!>@EqualsAndHashCode<!>
class EqualsAndHashCodeExampleWarning(val x: Int)

<!FLAG_USAGE_WARNING!>@Builder<!>
class BuilderExampleWarning(val x: Int)

// `@SuperBuilder` isn't supported on Kotlin-origin classes yet (ANNOTATION_IS_NOT_SUPPORTED),
// but `lombok.superBuilder.flagUsage` is still wired up and fires independently.
<!ANNOTATION_IS_NOT_SUPPORTED, FLAG_USAGE_WARNING!>@SuperBuilder<!>
class SuperBuilderExampleWarning

class NotAnnotated

// FILE: lombok.config

lombok.log.flagUsage=warning
lombok.toString.flagUsage=warning
lombok.equalsAndHashCode.flagUsage=warning
lombok.builder.flagUsage=warning
lombok.superBuilder.flagUsage=warning
