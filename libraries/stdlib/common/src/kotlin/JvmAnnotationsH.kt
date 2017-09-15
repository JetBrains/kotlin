package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FILE)
expect annotation class JvmName(val name: String)

@Target(FILE)
expect annotation class JvmMultifileClass

expect annotation class JvmField
