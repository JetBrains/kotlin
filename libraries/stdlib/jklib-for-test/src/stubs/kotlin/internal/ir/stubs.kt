/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.ir

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public annotation class FlexibleArrayElementVariance

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public annotation class FlexibleMutability

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public annotation class FlexibleNullability
