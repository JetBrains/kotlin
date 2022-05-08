/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

import kotlin.experimental.ExperimentalObjCName

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
 * See [NEW_MM.md#freezing-deprecation](https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md#freezing-deprecation) for details
 */
// Note: when changing level of deprecation here, also change
// * `freezing` mode handling in KonanConfig.kt
// * frontend diagnostics in ErrorsNative.kt
@SinceKotlin("1.7")
@RequiresOptIn(
    message = "Freezing API is deprecated since 1.7.20. See https://github.com/JetBrains/kotlin/blob/master/kotlin-native/NEW_MM.md#freezing-deprecation for details",
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
public expect annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)
