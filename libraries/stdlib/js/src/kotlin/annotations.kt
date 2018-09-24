/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.annotation.AnnotationTarget.*

@Target(CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, VALUE_PARAMETER, PROPERTY_GETTER, PROPERTY_SETTER)
@Deprecated("Use `external` modifier instead", level = DeprecationLevel.ERROR)
public annotation class native(public val name: String = "")

@Target(FUNCTION)
@Deprecated("Use inline extension function with body using dynamic")
public annotation class nativeGetter

@Target(FUNCTION)
@Deprecated("Use inline extension function with body using dynamic")
public annotation class nativeSetter

@Target(FUNCTION)
@Deprecated("Use inline extension function with body using dynamic")
public annotation class nativeInvoke

@Target(CLASS, FUNCTION, PROPERTY)
internal annotation class library(public val name: String = "")

@Target(CLASS)
internal annotation class marker

/**
 * Gives a declaration (a function, a property or a class) specific name in JavaScript.
 *
 * This may be useful in the following cases:
 *
 *   * There are two functions for which the compiler gives same name in JavaScript, you can
 *     mark one with `@JsName(...)` to prevent the compiler from reporting error.
 *   * You are writing a JavaScript library in Kotlin. The compiler produces mangled names
 *     for functions with parameters, which is unnatural for usual JavaScript developer.
 *     You can put `@JsName(...)` on functions you want to be available from JavaScript.
 *   * For some reason you want to rename declaration, e.g. there's common term in JavaScript
 *     for a concept provided by the declaration, which in uncommon in Kotlin.
 *
 * Example:
 *
 * ``` kotlin
 * class Person(val name: String) {
 *     fun hello() {
 *         println("Hello $name!")
 *     }
 *
 *     @JsName("helloWithGreeting")
 *     fun hello(greeting: String) {
 *         println("$greeting $name!")
 *     }
 * }
 * ```
 *
 * @property name the name which compiler uses both for declaration itself and for all references to the declaration.
 *           It's required to denote a valid JavaScript identifier.
 *
 */
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
public actual annotation class JsName(actual val name: String)

/**
 * Denotes an `external` declaration that must be imported from native JavaScript library.
 *
 * The compiler produces the code relevant for the target module system, for example, in case of CommonJS,
 * it will import the declaration via the `require(...)` function.
 *
 * The annotation can be used on top-level external declarations (classes, properties, functions) and files.
 * In case of file (which can't be `external`) the following rule applies: all the declarations in
 * the file must be `external`. By applying `@JsModule(...)` on a file you tell the compiler to import a JavaScript object
 * that contain all the declarations from the file.
 *
 * Example:
 *
 * ``` kotlin
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
 *
 * @see JsNonModule
 */
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, PROPERTY, FUNCTION, FILE)
public annotation class JsModule(val import: String)

/**
 * Denotes an `external` declaration that can be used without module system.
 *
 * By default, an `external` declaration is available regardless your target module system.
 * However, by applying [JsModule] annotation you can make a declaration unavailable to *plain* module system.
 * Some JavaScript libraries are distributed both as a standalone downloadable piece of JavaScript and as a module available
 * as an npm package.
 * To tell the Kotlin compiler to accept both cases, you can augment [JsModule] with the `@JsNonModule` annotation.
 *
 * For example:
 *
 * ``` kotlin
 * @JsModule("jquery")
 * @JsNonModule
 * @JsName("$")
 * external abstract class JQuery() {
 *     // some declarations here
 * }
 *
 * @JsModule("jquery")
 * @JsNonModule
 * @JsName("$")
 * external fun JQuery(element: Element): JQuery
 * ```
 *
 * @see JsModule
 */
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, PROPERTY, FUNCTION, FILE)
public annotation class JsNonModule

/**
 * Adds prefix to `external` declarations in a source file.
 *
 * JavaScript does not have concept of packages (namespaces). They are usually emulated by nested objects.
 * The compiler turns references to `external` declarations either to plain unprefixed names (in case of *plain* modules)
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
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FILE)
public annotation class JsQualifier(val value: String)