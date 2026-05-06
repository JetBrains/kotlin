// ISSUE: KT-81622
// FULL_JDK
// WITH_STDLIB

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j

@Log
<!LOG_PROPERTY_ALREADY_EXISTS!>@Slf4j<!>
class MultipleLogAnnotations
