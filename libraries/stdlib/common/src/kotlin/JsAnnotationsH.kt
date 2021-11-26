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
 * or with the `-opt-in=kotlin.js.ExperimentalJsExport` compiler option is given.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.4")
public annotation class ExperimentalJsExport

/**
 * Exports top-level declaration on JS platform.
 *
 * Compiled module exposes declarations that are marked with this annotation without name mangling.
 *
 * This annotation can be applied to either files or top-level declarations.
 *
 * It is currently prohibited to export the following kinds of declarations:
 *
 *   * `expect` declarations
 *   * inline functions with reified type parameters
 *   * suspend functions
 *   * secondary constructors without `@JsName`
 *   * extension properties
 *   * enum classes
 *   * annotation classes
 *
 * Signatures of exported declarations must only contain "exportable" types:
 *
 *   * `dynamic`, `Any`, `String`, `Boolean`, `Byte`, `Short`, `Int`, `Float`, `Double`
 *   * `BooleanArray`, `ByteArray`, `ShortArray`, `IntArray`, `FloatArray`, `DoubleArray`
 *   * `Array<exportable-type>`
 *   * Function types with exportable parameters and return types
 *   * `external` or `@JsExport` classes and interfaces
 *   * Nullable counterparts of types above
 *   * Unit return type. Must not be nullable
 *
 * This annotation is experimental, meaning that restrictions mentioned above are subject to change.
 */
@ExperimentalJsExport
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, PROPERTY, FUNCTION, FILE)
@SinceKotlin("1.4")
@OptionalExpectation
public expect annotation class JsExport()