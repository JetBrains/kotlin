/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

@Experimental(Experimental.Level.WARNING, [(Experimental.Impact.COMPILATION)])
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
annotation class JvmDefaultFeature

@JvmDefaultFeature
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class JvmDefault