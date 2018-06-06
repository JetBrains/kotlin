/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

// to compile tests in jre/jdk/7/8 projects where stdlib internals are unavailable
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoInfer
