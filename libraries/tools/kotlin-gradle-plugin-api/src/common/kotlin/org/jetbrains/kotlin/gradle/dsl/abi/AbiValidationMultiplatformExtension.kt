/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl

/**
 * @deprecated The class 'AbiValidationMultiplatformExtension' was removed.
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
@Suppress("DEPRECATION_ERROR")
@Deprecated("The class 'AbiValidationMultiplatformExtension' was removed.", level = DeprecationLevel.ERROR)
interface AbiValidationMultiplatformExtension : AbiValidationMultiplatformVariantSpec {
    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    val enabled: Property<Boolean>

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    @Suppress("DEPRECATION_ERROR")
    val variants: NamedDomainObjectContainer<AbiValidationMultiplatformVariantSpec>
}

/**
 * @deprecated The class 'AbiValidationMultiplatformVariantSpec' was removed.
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
@Deprecated("The class 'AbiValidationMultiplatformVariantSpec' was removed.", level = DeprecationLevel.ERROR)
interface AbiValidationMultiplatformVariantSpec {
    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    @Suppress("DEPRECATION_ERROR")
    val klib: AbiValidationKlibKindExtension

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    @Suppress("DEPRECATION_ERROR")
    fun klib(action: Action<AbiValidationKlibKindExtension>) {
        // no-op
    }
}

/**
 * The configuration for dumping declarations from non-JVM and non-Android targets compiled into klibs.
 *
 * @since 2.1.20
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
@Deprecated("The class 'AbiValidationKlibKindExtension' was removed.", level = DeprecationLevel.ERROR)
interface AbiValidationKlibKindExtension {
    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    val enabled: Property<Boolean>

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    val keepUnsupportedTargets: Property<Boolean>
}
