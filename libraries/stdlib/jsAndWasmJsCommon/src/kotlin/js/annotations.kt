/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.2")
@OptionalExpectation
public expect annotation class ExperimentalWasmJsInterop

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
public expect annotation class JsModule(val import: String)

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
public expect annotation class JsQualifier(val value: String)
