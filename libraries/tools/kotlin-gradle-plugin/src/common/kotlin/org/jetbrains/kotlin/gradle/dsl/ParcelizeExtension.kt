/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import javax.inject.Inject

/**
 * Configures the Kotlin Parcelize compiler plugin.
 */
@KotlinGradlePluginDsl
abstract class ParcelizeExtension @Inject internal constructor() {
    /**
     * Makes `@Parcelize` declarations implement `android.os.Parcelable` when it is not a direct supertype.
     * Thus, `: Parcelable` is optional on `@Parcelize` declarations.
     *
     * This also works in common source sets of multiplatform projects.
     * Android code sees their `@Parcelize` declarations as `Parcelable`.
     *
     * Defaults to `false`.
     */
    @ExperimentalKotlinGradlePluginApi
    abstract val implicitlyAddParcelableSupertype: Property<Boolean>
}
