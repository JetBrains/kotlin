// FIR_IDENTICAL
// WITH_STDLIB

// FILE: annotation.kt
package kotlinx.serialization

import kotlin.annotation.*

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepGeneratedSerializer

// FILE: test.kt
import kotlinx.serialization.*

@Serializable
data class WithTransients(<!TRANSIENT_MISSING_INITIALIZER!>@Transient val missing: Int<!>) {
    <!TRANSIENT_IS_REDUNDANT!>@Transient<!> val redundant: Int get() = 42

    @Transient
    lateinit var allowTransientLateinitWithoutInitializer: String
}

@Serializer(forClass = WithTransientsKept::class)
object WithTransientsKeptSerializer

@Serializable(WithTransientsKeptSerializer::class)
@KeepGeneratedSerializer
data class WithTransientsKept(<!TRANSIENT_MISSING_INITIALIZER!>@Transient val missing: Int<!>) {
    <!TRANSIENT_IS_REDUNDANT!>@Transient<!> val redundant: Int get() = 42

    @Transient
    lateinit var allowTransientLateinitWithoutInitializer: String
}
