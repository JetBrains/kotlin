/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * This annotation marks the experimental [ObjCEnum][kotlin.native.ObjCEnum] annotation that is considered experimental and is not subject to the
 * [general compatibility guarantees](https://kotlinlang.org/docs/reference/evolution/components-stability.html) given for the standard library:
 * the behavior of such annotation may be changed or the annotation may be removed completely in any further release.
 *
 * > Beware using the annotated annotation especially if you're developing a library, since your library might become binary incompatible
 * with the future versions of the standard library.
 */
@RequiresOptIn
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.3")
public annotation class ExperimentalObjCEnum
