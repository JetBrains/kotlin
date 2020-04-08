typealias ValidationRule2 = String
typealias ValidationRule3 = ValidationRule2
typealias ValidationRule4 = ValidationRule3

fun getValidator(): ValidationRule4<caret> = "abc"