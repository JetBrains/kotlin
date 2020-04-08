package org.jetbrains.annotations

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FIELD)
public annotation class PropertyKey(public val resourceBundle: String)