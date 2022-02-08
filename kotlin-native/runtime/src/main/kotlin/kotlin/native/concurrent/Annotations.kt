/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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
 * Marks a top level property with a backing field as immutable.
 * It is possible to share the value of such property between multiple threads, but it becomes deeply frozen,
 * so no changes can be made to its state or the state of objects it refers to.
 *
 * The annotation has effect only in Kotlin/Native platform.
 *
 * PLEASE NOTE THAT THIS ANNOTATION MAY GO AWAY IN UPCOMING RELEASES.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public actual annotation class SharedImmutable