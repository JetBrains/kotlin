// FULL_JDK

// FILE: test.kt

import lombok.ToString

// Warning: config sets the doNotUseGetters, report on any usage
<!TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT!>@ToString<!>
class Example(val x: Int)

// FILE: lombok.config
lombok.toString.doNotUseGetters=true
