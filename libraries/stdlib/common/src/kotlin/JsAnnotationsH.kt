/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.annotation.AnnotationTarget.*
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.reflect.KClass

/**
 * Gives a declaration (a function, a property or a class) specific name in JavaScript.
 *
 * In Kotlin/Wasm, interoperability with JavaScript is experimental, and the behavior of this annotation may change in the future.
 */
@Target(CLASS, FUNCTION, PROPERTY, CONSTRUCTOR, PROPERTY_GETTER, PROPERTY_SETTER)
@OptionalExpectation
public expect annotation class JsName(val name: String)

/**
 * Marks experimental [JsFileName] annotation.
 *
 * Note that behavior of these annotations will likely be changed in the future.
 *
 * Usages of such annotations will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalJsFileName::class)`,
 * or with the `-opt-in=kotlin.js.ExperimentalJsFileName` compiler option is given.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.9")
public annotation class ExperimentalJsFileName

/**
 * Specifies the name of the compiled file produced from the annotated source file instead of the default one.
 *
 * This annotation can be applied only to files and only when the compilation granularity is `PER_FILE`.
 */
@Target(FILE)
@OptionalExpectation
@ExperimentalJsFileName
@Retention(AnnotationRetention.SOURCE)
@SinceKotlin("1.9")
public expect annotation class JsFileName(val name: String)

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
 * Marks the experimental JsStatic annotation.
 *
 * Note that behavior of these annotations will likely be changed in the future.
 *
 * Usages of such annotations will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalJsStatic::class)`,
 * or with the `-opt-in=kotlin.js.ExperimentalJsStatic` compiler option is given.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.0")
public annotation class ExperimentalJsStatic

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
public expect annotation class JsExport() {
    /**
     * The annotation prevents exporting the annotated member of an exported class.
     * This annotation is experimental, meaning that the restrictions mentioned above are subject to change.
     */
    @ExperimentalJsExport
    @Retention(AnnotationRetention.BINARY)
    @Target(CLASS, PROPERTY, FUNCTION, CONSTRUCTOR)
    @SinceKotlin("1.8")
    @OptionalExpectation
    public annotation class Ignore()

    /**
     * The annotation notifies that the exported declaration should be exported as `default` on the JS platform.
     * It means that for ES modules the annotated declaration will be available under the `default` export.
     * For CommonJS, UMD, and plain modules the annotated declaration will be available by the name `default`.
     * This annotation is experimental, meaning that the restrictions mentioned above are subject to change.
     *
     * Note that if the annotation is applied multiple times across the project, the behavior depends on the compilation granularity.
     * For whole-program: if inside multiple libs the annotation is applied, it leads to a runtime error.
     * For per-module: the case with the `whole-program` defaults across dependencies is solved.
     * However, there will be the same runtime error if the annotation is applied multiple times in a single module.
     * For per-file: both problems of `whole-program` and `per-module` are solved,
     * but another one appear if @JsExport.Default applied multiple times in a single file
     */
    @ExperimentalJsExport
    @Retention(AnnotationRetention.BINARY)
    @Target(CLASS, PROPERTY, FUNCTION)
    @SinceKotlin("2.3")
    @OptionalExpectation
    public annotation class Default()
}


/**
 * This annotation marks the experimental Kotlin/JS reflection API that allows to create an instance of provided [KClass]
 * The API can be removed completely in any further release.
 *
 * Any usage of a declaration annotated with `@ExperimentalJsReflectionCreateInstance` should be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalJsReflectionCreateInstance::class)`,
 * or by using the compiler argument `-opt-in=kotlin.js.ExperimentalJsReflectionCreateInstance`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
@SinceKotlin("1.9")
public annotation class ExperimentalJsReflectionCreateInstance

/**
 * This annotation marks the experimental JS-collections API that allows to manipulate with native JS-collections
 * The API can be removed completely in any further release.
 *
 * Any usage of a declaration annotated with `@ExperimentalJsCollectionsApi` should be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalJsCollectionsApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.js.ExperimentalJsCollectionsApi`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@SinceKotlin("2.0")
public annotation class ExperimentalJsCollectionsApi

/**
 * The annotation is needed for annotating class declarations and type alias which are used inside exported declarations, but
 * doesn't contain @JsExport annotation
 * This information is used for generating special tagged types inside d.ts files, for more strict usage of implicitly exported entities
 */
@Target(AnnotationTarget.CLASS)
@UsedFromCompilerGeneratedCode
internal annotation class JsImplicitExport(val couldBeConvertedToExplicitExport: Boolean)

/**
 * Specifies that an additional static method is generated from the annotated companion object member if it's a function.
 * If the member is a property, additional static getter/setter methods are generated.
 */
@ExperimentalJsStatic
@Retention(AnnotationRetention.BINARY)
@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER)
@MustBeDocumented
@OptionalExpectation
@SinceKotlin("2.0")
public expect annotation class JsStatic()
