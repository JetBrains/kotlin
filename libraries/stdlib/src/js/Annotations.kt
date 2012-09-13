package js

/**
 * This annotation marks code as being a native JavaScript expression
 */
native
annotation class native(name : String = "")

/**
 * Represents a function in the standard library
 */
native
annotation class library(name : String = "")
