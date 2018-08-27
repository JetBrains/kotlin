/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.jvm

// Dummy JVM specific annotations in Kotlin/Native. Used in common stdlib.

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
actual annotation class JvmName(actual val name: String)

@Target(AnnotationTarget.FILE)
actual annotation class JvmMultifileClass

actual annotation class JvmField

@Target(AnnotationTarget.FIELD)
actual annotation class Volatile
