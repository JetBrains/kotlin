// FIR_IDENTICAL
// SKIP_TXT

// WITH_STDLIB
// FULL_JDK

import kotlinx.serialization.*

<!INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE!>@InheritableSerialInfo<!>
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Repeatable
annotation class RepeatableSerialInfo(val value: Int)

<!INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE!>@InheritableSerialInfo<!>
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@java.lang.annotation.Repeatable(JavaRepeatableContainer::class)
annotation class JavaRepeatable(val value2: Int)

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class JavaRepeatableContainer(val value: Array<JavaRepeatable>)

