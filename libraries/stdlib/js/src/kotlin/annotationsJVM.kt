/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

// these are used in common generated code in stdlib

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmOverloads

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-19507
internal actual annotation class JvmName(public actual val name: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-19507
internal actual annotation class JvmMultifileClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-19507
internal actual annotation class JvmField


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class Volatile

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
public annotation class Synchronized
