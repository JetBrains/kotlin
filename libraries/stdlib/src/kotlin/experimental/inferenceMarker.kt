/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/**
 * Marker of the use experimental type inference features
 */
@Experimental(level = Experimental.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS)
@SinceKotlin("1.3")
public annotation class ExperimentalTypeInference
