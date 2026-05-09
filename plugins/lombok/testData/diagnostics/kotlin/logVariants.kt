// ISSUE: KT-81622
// FULL_JDK
// WITH_STDLIB

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.extern.log4j.Log4j
import lombok.extern.apachecommons.CommonsLog
import lombok.extern.flogger.Flogger

@Log
<!LOG_PROPERTY_ALREADY_EXISTS!>@Slf4j<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@Log4j<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@CommonsLog<!>
<!LOG_PROPERTY_ALREADY_EXISTS!>@Flogger<!>
class MultipleLogAnnotations
