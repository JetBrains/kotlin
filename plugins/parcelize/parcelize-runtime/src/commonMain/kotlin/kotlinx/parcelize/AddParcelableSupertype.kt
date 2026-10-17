/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.parcelize

/**
 * Instructs the Kotlin compiler to add `android.os.Parcelable` as a supertype of the annotated declaration.
 * This is intended for multiplatform code, where `android.os.Parcelable` is not accessible from common source sets.
 *
 * The supertype is only added in compilations where the Parcelize compiler plugin is enabled (i.e., Android ones);
 * on other platforms the annotation has no effect.
 *
 * The annotation can be used independently of [Parcelize].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AddParcelableSupertype
