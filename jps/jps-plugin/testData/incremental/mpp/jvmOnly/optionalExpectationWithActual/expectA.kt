package org.example

@OptIn(kotlin.ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal expect annotation class A
