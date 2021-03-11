// "Replace with array call" "true"
// LANGUAGE_VERSION: 1.2

annotation class Some(vararg val strings: String)

@Some(strings = <caret>"value")
class My
