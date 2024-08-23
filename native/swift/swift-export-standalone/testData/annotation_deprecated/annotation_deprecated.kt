// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: packageRoot=org.kotlin.foo

package org.kotlin.foo

@Deprecated("This function is deprecated")
fun deprecatedFunction() {}

@Deprecated("This function is deprecated and hidden", level = DeprecationLevel.HIDDEN)
fun hiddenDeprecatedFunction() {}

@Deprecated("This function is deprecated and will cause an error if used", level = DeprecationLevel.ERROR)
fun errorDeprecatedFunction() {}

@Deprecated("This class is deprecated")
class DeprecatedClass

@Deprecated("This class is deprecated and hidden", level = DeprecationLevel.HIDDEN)
class HiddenDeprecatedClass

@Deprecated("This class is deprecated and will cause an error if used", level = DeprecationLevel.ERROR)
class ErrorDeprecatedClass

class ExampleClass {
    @Deprecated("This method is deprecated and hidden", level = DeprecationLevel.HIDDEN)
    fun hiddenDeprecatedMethod() {}

    @Deprecated("This method is deprecated and will cause an error if used", level = DeprecationLevel.ERROR)
    fun errorDeprecatedMethod() {}
}

@Deprecated("This property is deprecated")
val deprecatedProperty: String
    get() = "This is deprecated"

@Deprecated("This property is deprecated and hidden", level = DeprecationLevel.HIDDEN)
val hiddenDeprecatedProperty: String
    get() = "This is hidden"

@Deprecated("This property is deprecated and will cause an error if used", level = DeprecationLevel.ERROR)
val errorDeprecatedProperty: String
    get() = "This will cause an error"