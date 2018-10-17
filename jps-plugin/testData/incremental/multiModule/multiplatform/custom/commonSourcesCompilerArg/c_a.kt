@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")

@OptionalExpectation
expect annotation class Optional(val value: String)

@Optional("Foo")
class Foo