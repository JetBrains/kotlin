/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.annotation.AnnotationTarget.*

/**
 * Gives a declaration (a function, a property or a class) specific name in JavaScript.
 */
@Target(CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
@OptionalExpectation
public expect annotation class JsName(val name: String)

/**
 * Marks experimental JS export annotations.
 *
 * Note that behavior of these annotations will likely be changed in the future.
 *
 * Usages of such annotations will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalJsExport::class)`,
 * or with the `-Xopt-in=kotlin.js.ExperimentalJsExport` compiler option is given.
 */
@Suppress("DEPRECATION")
@Experimental(level = Experimental.Level.WARNING)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.4")
public annotation class ExperimentalJsExport

/**
 * Exports top-level declaration.
 *
 * Compiler exports from the module those top-level declarations that are marked with this annotation.
 * There is no effect if this annotation is applied to a non-top-level declaration.
 *
 * This annotation has effect only on top-level declarations and only in IR-based JS backend.
 */
@ExperimentalJsExport
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, PROPERTY, FUNCTION, FILE)
@SinceKotlin("1.4")
@OptionalExpectation
public expect annotation class JsExport()