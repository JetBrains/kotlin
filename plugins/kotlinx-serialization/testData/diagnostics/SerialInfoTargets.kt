// WITH_STDLIB
// FULL_JDK

import kotlinx.serialization.*

// @SerialInfo applicable to class/property targets is fine
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class SerialInfoOnClassAndProperty(val value: Int)

// @SerialInfo restricted to a single allowed target is fine
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialInfoOnProperty(val value: Int)

// @SerialInfo without explicit @Target: default targets include inapplicable ones (field, parameter, ...) -> reported
@SerialInfo
annotation class SerialInfoNoTarget(val value: Int)

// @SerialInfo applicable to a TYPE target is reported
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class SerialInfoOnType(val value: Int)

// @SerialInfo applicable to a value-parameter target is reported
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class SerialInfoOnParameter(val value: Int)

// @SerialInfo applicable to an annotation-class target is reported
@SerialInfo
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SerialInfoOnAnnotationClass(val value: Int)

// @InheritableSerialInfo restricted to the class target is fine
@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
annotation class InheritableOnClass(val value: Int)

// @InheritableSerialInfo without explicit @Target: default targets include property etc. -> reported
@InheritableSerialInfo
annotation class InheritableNoTarget(val value: Int)

// @InheritableSerialInfo applicable to a property target is reported
@InheritableSerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class InheritableOnProperty(val value: Int)
