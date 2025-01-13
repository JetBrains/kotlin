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
 * A plugin DSL extension for configuring Application Binary Interface (ABI) validation.
 *
 * ABI validation is a part of the Kotlin toolset designed to control which declarations are available to other modules.
 * Use this tool to control the binary compatibility of your library or shared module.
 *
 * This extension is available inside the `kotlin {}` block in your build script:
 *
 * ```kotlin
 * kotlin {
 *     abiValidation {
 *         // Your ABI validation configuration
 *     }
 * }
 * ```
 *
 *  Note that this DSL is experimental, and it will likely change in future versions until it is stable.
 *
 * @since 2.1.20
 */
/*
We can't mark top level extensions with @ExperimentalAbiValidation because
in buildSrc Gradle always creates accessors for these extensions which cause the opt-in error,
which cannot be suppressed.

See Gradle issue https://github.com/gradle/gradle/issues/32019
 */
@KotlinGradlePluginDsl
interface AbiValidationMultiplatformExtension : AbiValidationMultiplatformVariantSpec {
    /**
     * All ABI validation report variants that are available in this project.
     *
     * See [AbiValidationMultiplatformVariantSpec] for more details about report variants.
     *
     * By default, each project always has one variant, called the main variant. It is named [AbiValidationVariantSpec.MAIN_VARIANT_NAME] and is configured in the `kotlin {}` block:
     *
     * ```kotlin
     * kotlin {
     *     abiValidation {
     *         // main variant configuration
     *     }
     * }
     * ```
     *
     * This is a live mutable collection. New custom variants can be created using special functions such as [NamedDomainObjectContainer.create] or [NamedDomainObjectContainer.register].
     * Variants can also be configured at the time of their creation:
     *
     * ```kotlin
     * kotlin {
     *     abiValidation {
     *         variants.register("my") {
     *             // 'my' variant configuration, not main
     *         }
     *     }
     * }
     * ```
     * Or later:
     *
     * ```kotlin
     * kotlin {
     *     abiValidation {
     *         variants.register("my")
     *     }
     * }
     * //
     * kotlin {
     *     abiValidation {
     *         variants.getByName("my").filters {
     *             // configure filters for 'my' variant
     *         }
     *     }
     * }
     * ```
     */
    @ExperimentalAbiValidation
    val variants: NamedDomainObjectContainer<AbiValidationMultiplatformVariantSpec>
}

/**
 * A specification for the ABI validation report variant.
 *
 * An ABI validation report variant is a group of configurations (like filters, klib validation, etc.), for which a separate set of Gradle tasks is created.
 * Different variants allow generating ABI dumps for different sets of classes and targets without modifying the build script.
 *
 * Each report variant has a unique name.
 *
 * A distinct set of Gradle tasks is created for each variant, with unique names.
 *
 * You can access tasks using properties:
 *
 * For the main variant:
 *
 * ```kotlin
 * kotlin {
 *     abiValidation {
 *         legacyDump.legacyDumpTaskProvider
 *         legacyDump.legacyCheckTaskProvider
 *         legacyDump.legacyUpdateTaskProvider
 *     }
 * }
 * ```
 * And for custom variants:
 *
 *```kotlin
 * kotlin {
 *     abiValidation {
 *         variants.getByName("my").legacyDump.legacyDumpTaskProvider
 *         variants.getByName("my").legacyDump.legacyCheckTaskProvider
 *         variants.getByName("my").legacyDump.legacyUpdateTaskProvider
 *     }
 * }
 * ```
 *
 * Note that this DSL is experimental, and it will likely change in future versions until it is stable.
 *
 * @since 2.1.20
 */
/*
We can't mark top level extensions with @ExperimentalAbiValidation because
in buildSrc Gradle always creates accessors for these extensions which cause the opt-in error,
which cannot be suppressed.

See Gradle issue https://github.com/gradle/gradle/issues/32019
 */
@KotlinGradlePluginDsl
interface AbiValidationMultiplatformVariantSpec : AbiValidationVariantSpec {
    /**
     * Configure storing declarations from non-JVM and non-Android targets which are compiled in klibs.
     */
    @ExperimentalAbiValidation
    val klib: AbiValidationKlibKindExtension

    /**
     * Configures the [klib] with the provided configuration.
     */
    @ExperimentalAbiValidation
    fun klib(action: Action<AbiValidationKlibKindExtension>) {
        action.execute(klib)
    }
}

/**
 * The configuration for dumping declarations from non-JVM and non-Android targets compiled into klibs.
 *
 * @since 2.1.20
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiValidationKlibKindExtension {
    /**
     * Whether to save declarations from a klib into the dump and check them.
     */
    val enabled: Property<Boolean>

    /**
     * Whether to include the declarations for targets which are not supported by host compiler in the generated dump. 
     * These declarations are taken from the reference dump, if available.
     *
     * If possible, unsupported targets are supplemented with common declarations that are already present in the supported targets.
     *
     * However, this does not provide a complete guarantee, so it should be used with caution.
     *
     * If the option is set to `false` and the compiler does not support some of the Kotlin targets used in the current project, the dump generation fails with an error.
     *
     * Default value: `true`
     */
    val keepUnsupportedTargets: Property<Boolean>
}
