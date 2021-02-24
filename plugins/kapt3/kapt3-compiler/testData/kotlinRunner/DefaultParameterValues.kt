// DUMP_DEFAULT_PARAMETER_VALUES

package test

@Anno
class User(val name: String = "John", age: Int = 18)

internal annotation class Anno