/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks the JVM backing field of the annotated `var` property as `volatile`, meaning that reads and writes to this field
 * are atomic and writes are always made visible to other threads. If another thread reads the value of this field (e.g. through its accessor),
 * it sees not only that value, but all side effects that led to writing that value.
 *
 * Note that only _backing field_ operations are atomic when the field is annotated with `Volatile`.
 * For example, if the property getter or setter make several operations with the backing field,
 * a _property_ operation, i.e. reading or setting it through these accessors, is not guaranteed to be atomic.
 */
@Target(FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Volatile

/**
 * Marks the backing field of the annotated property with the `transient` modifier on the JVM platform, meaning that it is not
 * a part of the serialized form of the object when serialized with `java.io.Serializable` machinery.
 *
 * **Warning:** the `java.io.Serializable` is an unsound mechanism that bypasses classes' invariants.
 * When `@Transient` annotation is applied to a property, the author must ensure that either the property has a nullable type
 * or that an author-supplied `readResolve` is implemented, supplying a conforming value for the non-nullable transient property.
 *
 * See also: ["Java Object Serialization Specification"](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/index.html)
 */
@Target(FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Transient

/**
 * Marks the JVM method generated from the annotated function as `strictfp`, meaning that the precision
 * of floating point operations performed inside the method needs to be restricted in order to
 * achieve better portability.
 */
@Target(FUNCTION, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Strictfp

/**
 * Marks the JVM method generated from the annotated function as `synchronized`, meaning that the method
 * will be protected from concurrent execution by multiple threads by the monitor of the instance (or,
 * for static methods, the class) on which the method is defined.
 *
 * Note that for an extension function, the monitor of the facade class, where it gets compiled to a static method, is used.
 * Therefore, this annotation is recommended to be applied only to member functions and properties.
 * In other cases, use [synchronized] function and explicitly specify the lock to be used.
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Synchronized

/**
 * Makes the annotated lambda function implement `java.io.Serializable`,
 * generates a pretty `toString` implementation and adds reflection metadata.
 */
@Target(EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@SinceKotlin("1.8")
public actual annotation class JvmSerializableLambda