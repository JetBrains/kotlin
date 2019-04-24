/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * The experimental marker for type inference augmenting annotations.
 *
 * Any usage of a declaration annotated with `@ExperimentalTypeInference` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalTypeInference::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.experimental.JvmUnchecked`.
 */
@Experimental(level = Experimental.Level.WARNING)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
@SinceKotlin("1.3")
public annotation class JvmUnchecked
