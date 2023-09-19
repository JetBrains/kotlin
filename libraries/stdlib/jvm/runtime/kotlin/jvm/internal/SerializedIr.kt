/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

/**
 * This annotation is used to store serialized IR data inside classfiles.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@SinceKotlin("1.6")
public annotation class SerializedIr(
    @get:JvmName("b")
    val bytes: Array<String> = []
)
