/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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
public actual annotation class ThreadLocal

/**
 * Note: this annotation has effect only in Kotlin/Native with legacy memory manager.
 *
 * Marks a top level property with a backing field as immutable.
 * It is possible to share the value of such property between multiple threads, but it becomes deeply frozen,
 * so no changes can be made to its state or the state of objects it refers to.
 *
 * PLEASE NOTE THAT THIS ANNOTATION MAY GO AWAY IN UPCOMING RELEASES.
 *
 * Since 1.7.20 usage of this annotation is deprecated.
 * See https://kotlinlang.org/docs/native-migration-guide.html for details.
 */
@Deprecated("This annotation is redundant and has no effect")
@DeprecatedSinceKotlin(warningSince = "1.9")
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
// Not @FreezingIsDeprecated: Lots of usages, only the doc updated.
public actual annotation class SharedImmutable