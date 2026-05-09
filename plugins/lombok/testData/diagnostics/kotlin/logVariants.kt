// ISSUE: KT-81622
// FULL_JDK
// WITH_STDLIB

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.extern.apachecommons.CommonsLog
import lombok.extern.flogger.Flogger
import lombok.extern.jbosslog.JBossLog
import lombok.extern.log4j.Log4j2
import lombok.extern.slf4j.XSlf4j

@Log
<!LOG_PROPERTY_ALREADY_EXISTS!>@Slf4j<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@Log4j<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@CommonsLog<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@Flogger<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@JBossLog<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@Log4j2<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@XSlf4j<!>
class MultipleLogAnnotations
