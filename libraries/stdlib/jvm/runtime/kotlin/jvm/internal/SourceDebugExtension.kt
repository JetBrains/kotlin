/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

/**
 * Provides a copy of the JVM attribute SourceDebugExtension on the class file.
 * This annotation exists if and only if there is a SourceDebugExtension attribute on the class.
 * To obtain the stored source mapping information, concatenate the strings in [value].
 * This annotation is needed for tools which inspect the Kotlin bytecode via JVMTI,
 * which does not always provide access to the SourceDebugExtension attribute.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.8")
public annotation class SourceDebugExtension(val value: Array<String>)
