/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.reflect.KClass

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a platform method
 * in Kotlin/JVM and Kotlin/Native.
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@SinceKotlin("1.4")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)
