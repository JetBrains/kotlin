/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.annotation.AnnotationTarget.*

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
@SinceKotlin("1.3")
public actual annotation class JsExport {
    /*
    * The annotation prevents exporting the annotated member of an exported class.
    * This annotation is experimental, meaning that the restrictions mentioned above are subject to change.
    */
    @ExperimentalJsExport
    @Retention(AnnotationRetention.BINARY)
    @Target(CLASS, PROPERTY, FUNCTION, CONSTRUCTOR)
    @SinceKotlin("1.8")
    public actual annotation class Ignore
}


/**
 * Forces a top-level property to be initialized eagerly, opposed to lazily on the first access to file and/or property.
 */
@ExperimentalStdlibApi
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
@SinceKotlin("1.6")
@Deprecated("This annotation is a temporal migration assistance and may be removed in the future releases, please consider filing an issue about the case where it is needed")
public annotation class EagerInitialization


/**
 * When placed on an external interface or class, requires all its child
 * interfaces, classes, and objects to be external as well.
 *
 * The compiler mangles identifiers of functions and properties from non-external interfaces and classes,
 * and doesn't mangle from external ones. Requiring external interfaces and classes inheritors being external
 * is necessary to avoid non-obvious bugs when identifier naming in an inheritor doesn't correspond to
 * naming in the base class or interface.
 *
 * Example:
 *
 * ```kotlin
 * // From kotlin-wrappers
 * external interface Props {
 *     var key: Key?
 * }
 *
 * // User code
 * @OptIn(ExperimentalStdlibApi::class)
 * @JsExternalInheritorsOnly
 * external interface LayoutProps : Props {
 *     var layout: Layout?
 * }
 *
 * external interface ComponentProps : LayoutProps // OK
 *
 * external interface ComponentA : ComponentProps // OK
 *
 * interface ComponentB : ComponentProps // Compilation error!
 * ```
 *
 * This annotation is experimental, meaning that the restrictions mentioned above are subject to change.
 */
@ExperimentalStdlibApi
@Retention(AnnotationRetention.BINARY)
@Target(CLASS)
@SinceKotlin("1.9")
public annotation class JsExternalInheritorsOnly
