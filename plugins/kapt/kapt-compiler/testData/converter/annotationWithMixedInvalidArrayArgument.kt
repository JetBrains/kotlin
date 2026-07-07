// WITH_STDLIB
@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST", "TYPE_MISMATCH")

annotation class A(val value: Array<String>)

@A(value = [Missing::class, "ok"])
class Test
