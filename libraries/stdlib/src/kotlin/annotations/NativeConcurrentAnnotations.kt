/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.concurrent

/**
 * Marks a top level property with a backing field or an object as thread local.
 * The object remains mutable and it is possible to change its state,
 * but every thread will have a distinct copy of this object,
 * so changes in one thread are not reflected in another.
 *
 * The annotation has effect only in Kotlin/Native platform.
 *
 * PLEASE NOTE THAT THIS ANNOTATION MAY GO AWAY IN UPCOMING RELEASES.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public expect annotation class ThreadLocal()

/**
 * This annotation has no effect, and its usages can be safely dropped.
 *
 * Since 1.7.20 usage of this annotation is deprecated. See https://kotlinlang.org/docs/native-migration-guide.html for details.
 */
@Deprecated("This annotation is redundant and has no effect")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public expect annotation class SharedImmutable()

