/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

// This is here instead of kotlin-native/runtime because some of native-wasm uses this annotation.
/**
 * Freezing API has been deprecated since Kotlin 1.7.20,
 * and support for the legacy memory manager was completely removed from the compiler in 1.9.20.
 *
 * See the [documentation](https://kotlinlang.org/docs/native-migration-guide.html) for details.
 */
@SinceKotlin("1.7")
@RequiresOptIn(
    message = "Freezing API is deprecated since 1.7.20. See https://kotlinlang.org/docs/native-migration-guide.html for details",
    level = RequiresOptIn.Level.WARNING
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
@MustBeDocumented
@Deprecated("Opting in for the freezing API is no longer supported.")
@DeprecatedSinceKotlin(warningSince = "2.1")
public actual annotation class FreezingIsDeprecated
