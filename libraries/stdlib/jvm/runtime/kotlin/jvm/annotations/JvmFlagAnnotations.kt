/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ACTUAL_WITHOUT_EXPECT") // for building kotlin-runtime
package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks the JVM backing field of the annotated property as `volatile`, meaning that writes to this field
 * are immediately made visible to other threads.
 */
@Target(FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Volatile

/**
 * Marks the JVM backing field of the annotated property as `transient`, meaning that it is not
 * part of the default serialized form of the object.
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
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public actual annotation class Synchronized