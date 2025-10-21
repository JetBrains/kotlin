// KIND: STANDALONE
// MODULE: foo
// FILE: foo.kt

@RequiresOptIn(message = "This is an experimental API")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MyExperimentalApi

@MyExperimentalApi
class ExperimentalClass(val value: Int)

@MyExperimentalApi
fun experimentalFunction(): String = "experimental"

@MyExperimentalApi
val experimentalProperty: String = "experimental property"


// MODULE: bar(foo)
// EXPORT_TO_SWIFT
// OPT_IN: MyExperimentalApi
// FILE: bar.kt
@file:OptIn(MyExperimentalApi::class)

class ConsumerClass {
    val experimental: ExperimentalClass = ExperimentalClass(42)

    fun useExperimentalFunction(): String = experimentalFunction()

    fun useExperimentalProperty(): String = experimentalProperty
}

fun createExperimental(): ExperimentalClass = ExperimentalClass(100)

val derivedProperty: String = experimentalFunction() + experimentalProperty