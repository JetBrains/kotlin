/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind


/**
 * Marks the backing field of the annotated `var` property as `volatile`, meaning that reads and writes to this field
 * are atomic and writes are always made visible to other threads. If another thread reads the value of this field (e.g. through its accessor),
 * it sees not only that value, but all side effects that led to writing that value.
 *
 * This annotation has effect in Kotlin/JVM and Kotlin/Native.
 *
 * Note that only _backing field_ operations are atomic when the field is annotated with `Volatile`.
 * For example, if the property getter or setter make several operations with the backing field,
 * a _property_ operation, i.e. reading or setting it through these accessors, is not guaranteed to be atomic.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@OptionalExpectation
@SinceKotlin("1.9")
@RequireKotlin(version = "1.8.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
public expect annotation class Volatile()