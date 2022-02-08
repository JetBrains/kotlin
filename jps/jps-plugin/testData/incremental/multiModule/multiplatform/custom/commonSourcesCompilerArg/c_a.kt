@file:OptIn(kotlin.ExperimentalMultiplatform::class)

@OptionalExpectation
expect annotation class Optional(val value: String)

@Optional("Foo")
class Foo