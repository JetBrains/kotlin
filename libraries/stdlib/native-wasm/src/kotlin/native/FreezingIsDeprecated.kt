/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

// This is here instead of kotlin-native/runtime because some of native-wasm uses this annotation.
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
public actual annotation class FreezingIsDeprecated
