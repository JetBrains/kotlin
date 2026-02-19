// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt

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

// FILE: main.kt
import kotlinx.serialization.*

open class NonSerializableParent(val arg: Int)

<!NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR!>@Serializable<!>
class Derived(val someData: String): NonSerializableParent(42)

@Serializer(forClass = DerivedKept::class)
class DerivedKeptSerializer(val serializer: KSerializer<*>)

<!NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR!>@Serializable(DerivedKeptSerializer::class)<!>
@KeepGeneratedSerializer
class DerivedKept(val someData: String): NonSerializableParent(42)
