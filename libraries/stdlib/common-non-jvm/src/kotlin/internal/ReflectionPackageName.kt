/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Makes the compiler store [name] instead of the real package FQ name in the reflective information
 * of the classes declared in the annotated file.
 *
 * This annotation is used by the compiler grouping test infrastructure, which renames packages when several tests
 * are compiled together into one batch. Without it, `KClass.qualifiedName`, `KClass.toString()`
 * and the default `Any.toString()` would expose the batch package prefix.
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
@Suppress("unused")
internal annotation class ReflectionPackageName(val name: String)
