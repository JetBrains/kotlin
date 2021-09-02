@file:Suppress("OPT_IN_USAGE_ERROR")

@OptionalExpectation
expect annotation class Optional(val value: String)

@Optional("Foo")
class Foo