/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

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
