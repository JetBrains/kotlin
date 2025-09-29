/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * This annotation marks the experimental [ObjCEnum][kotlin.native.ObjCEnum] annotation.
 */
@RequiresOptIn
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.3")
public annotation class ExperimentalObjCEnum
