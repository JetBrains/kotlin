/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

import kotlin.experimental.ExperimentalObjCName
import kotlin.experimental.ExperimentalObjCRefinement

/**
 * Makes top level function available from C/C++ code with the given name.
 *
 * [externName] controls the name of top level function, [shortName] controls the short name.
 * If [externName] is empty, no top level declaration is being created.
 */
@SinceKotlin("1.5")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public expect annotation class CName(val externName: String = "", val shortName: String = "")

/**
 * Freezing API is deprecated since 1.7.20.
 *
 * See [documentation](https://kotlinlang.org/docs/native-migration-guide.html) for details
 */
// Note: when changing level of deprecation here, also change
// * `freezing` mode handling in KonanConfig.kt
// * frontend diagnostics in ErrorsNative.kt
@SinceKotlin("1.7")
@RequiresOptIn(
    message = "Freezing API is deprecated since 1.7.20. See https://kotlinlang.org/docs/native-migration-guide.html for details",
    level = RequiresOptIn.Level.WARNING,
)
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
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
expect annotation class FreezingIsDeprecated

/**
 * Instructs the Kotlin compiler to use a custom Objective-C and/or Swift name for this class, property, parameter or function.
 * @param exact specifies if the name of a class should be interpreted as the exact name.
 * E.g. the compiler won't add a top level prefix or the outer class names to exact names.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@OptionalExpectation
@ExperimentalObjCName
@SinceKotlin("1.8")
public expect annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

/**
 * Meta-annotation that instructs the Kotlin compiler to remove the annotated class, function or property from the public Objective-C API.
 *
 * Annotation processors that refine the public Objective-C API can annotate their annotations with this meta-annotation
 * to have the original declarations automatically removed from the public API.
 *
 * Note: only annotations with [AnnotationTarget.CLASS], [AnnotationTarget.FUNCTION] and/or [AnnotationTarget.PROPERTY] are supported.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@OptionalExpectation
@ExperimentalObjCRefinement
@SinceKotlin("1.8")
public expect annotation class HidesFromObjC()

/**
 * Instructs the Kotlin compiler to remove this class, function or property from the public Objective-C API.
 */
@HidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@OptionalExpectation
@ExperimentalObjCRefinement
@SinceKotlin("1.8")
public expect annotation class HiddenFromObjC()

/**
 * Meta-annotation that instructs the Kotlin compiler to mark the annotated function or property as
 * `swift_private` in the generated Objective-C API.
 *
 * Annotation processors that refine the public API in Swift can annotate their annotations with this meta-annotation
 * to automatically hide the annotated declarations from Swift.
 *
 * See Apple's documentation of the [`NS_REFINED_FOR_SWIFT`](https://developer.apple.com/documentation/swift/objective-c_and_c_code_customization/improving_objective-c_api_declarations_for_swift)
 * macro for more information on refining Objective-C declarations in Swift.
 *
 * Note: only annotations with [AnnotationTarget.FUNCTION] and/or [AnnotationTarget.PROPERTY] are supported.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@OptionalExpectation
@ExperimentalObjCRefinement
@SinceKotlin("1.8")
public expect annotation class RefinesInSwift()

/**
 * Instructs the Kotlin compiler to mark this function or property as `swift_private` in the generated Objective-C API.
 *
 * See Apple's documentation of the [`NS_REFINED_FOR_SWIFT`](https://developer.apple.com/documentation/swift/objective-c_and_c_code_customization/improving_objective-c_api_declarations_for_swift)
 * macro for more information on refining Objective-C declarations in Swift.
 */
@RefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@OptionalExpectation
@ExperimentalObjCRefinement
@SinceKotlin("1.8")
public expect annotation class ShouldRefineInSwift()

