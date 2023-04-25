/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.concurrent

/**
 * Marks all `Worker`-related API as obsolete.
 *
 * Workers are considered **obsolete**: their API shape, design, and behaviour
 * have known flaws and pitfalls that are impossible to address in Worker's current form.
 *
 * Workers continue being supported and maintained, but they eventually will
 * be replaced with threads API and then deprecated.
 *
 * It is not recommended to expose workers in public API or to have high coupling with Workers API,
 * and instead, we suggest encapsulating them in a domain-specific API to simplify
 * future migration and reduce potential exposure to dangerous API.
 */
@SinceKotlin("1.9")
@RequiresOptIn(
    message = "Workers API is obsolete and will be replaced with threads eventually",
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
public annotation class ObsoleteWorkersApi
