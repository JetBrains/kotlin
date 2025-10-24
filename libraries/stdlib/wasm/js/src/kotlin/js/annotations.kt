/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Exports top-level declaration on JS platform.
 *
 * Can only be applied to top-level functions.
 */
@ExperimentalJsExport
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
// All targets from expect can't be set because for K/Wasm so far you can export only functions
@Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
public actual annotation class JsExport {
    @ExperimentalJsExport
    @Retention(AnnotationRetention.BINARY)
    @Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.CONSTRUCTOR,
    )
    public actual annotation class Ignore


    /**
     * This annotation indicates that the exported declaration should be exported as `default` on the JS platform.
     *
     * In ES modules, the annotated declaration is available as the `default` export.
     * In CommonJS, UMD, and plain modules, the annotated declaration is available under the name `default`.
     *
     * This annotation is experimental, meaning that the restrictions described above are subject to change.
     *
     * Note: If the annotation is applied multiple times across the project, the behavior depends on the compilation granularity.
     * 
     * - **Whole-program compilation**: If multiple libraries apply the annotation, it results in a runtime error.
     * - **Per-module compilation**: Conflicts across dependencies (like in `whole-program` mode) are resolved.
     *   However, a runtime error occurs if the annotation is applied multiple times within a single module.
     * - **Per-file compilation**: This mode resolves the issues present in `whole-program` and `per-module` modes.
     *   However, a new issue arises if `@JsExport.Default` is applied multiple times within the same file.
     */
    @ExperimentalJsExport
    @Retention(AnnotationRetention.BINARY)
    @Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
    )
    @SinceKotlin("2.3")
    public actual annotation class Default
}

/**
 * Gives a declaration (a function, a property or a class) specific name in JavaScript.
 *
 * Interoperability with JavaScript is experimental, and the behavior of this annotation may change in the future.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
public actual annotation class JsName(actual val name: String)

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
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
public actual annotation class JsModule(actual val import: String)


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
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FILE)
public actual annotation class JsQualifier(actual val value: String)

/**
 *
 * Marks a member function of an external declaration as the "invoke operator" of a JavaScript object.
 * Every call to this function will be translated into a call of the object itself.
 *
 * Example:
 *
 * ```kotlin
 * external class A {
 *   @nativeInvoke
 *   operator fun invoke()
 * }
 *
 * fun main() {
 *   val a = A()
 *   a()
 * }
 * ```
 *
 */
@SinceKotlin("2.3")
@ExperimentalWasmJsInterop
@Target(FUNCTION)
@Deprecated("Temporary solution until WasmJs <-> Js interoperability will be designed. It may be removed in the future releases.")
public actual annotation class nativeInvoke()
