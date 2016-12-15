package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FILE)
header annotation class JvmName(val name: String)

@Target(FILE)
header annotation class JvmMultifileClass

header annotation class JvmField
