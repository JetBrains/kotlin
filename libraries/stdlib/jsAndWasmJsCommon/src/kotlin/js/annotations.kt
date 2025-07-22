/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Marks API related to interoperability with JS as experimental.
 *
 * Note that the behavior of such API may be changed in the future.
 *
 * Usages of such API will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalWasmJsInterop::class)`,
 * or with the `-opt-in=kotlin.js.ExperimentalWasmJsInterop` compiler option is given.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.2")
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
public annotation class ExperimentalWasmJsInterop

/**
 * Denotes an `external` declaration that must be imported from JavaScript module.
 *
 * The annotation can be used on top-level external declarations (classes, properties, functions) and files.
 * In case of file (which can't be `external`) the following rule applies: all the declarations in
 * the file must be `external`. By applying `@JsModule(...)` on a file you tell the compiler to import a JavaScript object
 * that contains all the declarations from the file.
 *
 * Example:
 *
 * ```kotlin
 * @JsModule("jquery")
 * external abstract class JQuery() {
 *     // some declarations here
 * }
 *
 * @JsModule("jquery")
 * external fun JQuery(element: Element): JQuery
 * ```
 *
 * @property import name of a module to import declaration from.
 *           It is not interpreted by the Kotlin compiler, it's passed as is directly to the target module system.
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
public expect annotation class JsModule(val import: String)

/**
 * Adds prefix to `external` declarations in a source file.
 *
 * JavaScript does not have a concept of packages (namespaces). They are usually emulated by nested objects.
 * The compiler turns references to `external` declarations either to plain unprefixed names
 * or to plain imports.
 * However, if a JavaScript library provides its declarations in packages, you won't be satisfied with this.
 * You can tell the compiler to generate additional prefix before references to `external` declarations using the `@JsQualifier(...)`
 * annotation.
 *
 * Note that a file marked with the `@JsQualifier(...)` annotation can't contain non-`external` declarations.
 *
 * Example:
 *
 * ```
 * @file:JsQualifier("my.jsPackageName")
 * package some.kotlinPackage
 *
 * external fun foo(x: Int)
 *
 * external fun bar(): String
 * ```
 *
 * @property value the qualifier to add to the declarations in the generated code.
 *           It must be a sequence of valid JavaScript identifiers separated by the `.` character.
 *           Examples of valid qualifiers are: `foo`, `bar.Baz`, `_.$0.f`.
 *
 * @see JsModule
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
public expect annotation class JsQualifier(val value: String)
