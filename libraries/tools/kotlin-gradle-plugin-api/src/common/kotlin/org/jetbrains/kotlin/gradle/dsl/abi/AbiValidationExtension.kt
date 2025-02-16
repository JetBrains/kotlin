/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl

/**
 * An *experimental* plugin DSL extension to configure Application Binary Interface (ABI) validation.
 *
 * ABI validation is a part of the Kotlin toolset designed to control which declarations are available to other modules.
 * You can use this tool to control the binary compatibility of your library or shared module.
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
interface AbiValidationExtension : AbiValidationVariantSpec {
    /**
     * All ABI validation report variants that are available in this project.
     *
     * See [AbiValidationVariantSpec] for more details about report variants.
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
    val variants: NamedDomainObjectContainer<AbiValidationVariantSpec>
}

/**
 *  A specification for the ABI validation report variant.
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
interface AbiValidationVariantSpec : Named {
    /**
     * A set of filtering rules that restrict Application Binary Interface (ABI) declarations from being included in a dump.
     *
     * The rules combine inclusion and exclusion of declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.classes]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]):
     *
     * ```kotlin
     * abiValidation {
     *     filters {
     *         excluded {
     *             classes.add("foo.Bar")
     *             annotatedWith.add("foo.ExcludeAbi")
     *         }
     *
     *         included {
     *             classes.add("foo.api.**")
     *             annotatedWith.add("foo.PublicApi")
     *         }
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property or function) to get into the dump, it must pass the inclusion **and** exclusion filters.
     *
     * A declaration successfully passes the exclusion filter if it does not match any of the class name (see [AbiFilterSetSpec.classes]) or annotation (see [AbiFilterSetSpec.annotatedWith]) filter rules.
     *
     * A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
     */
    @ExperimentalAbiValidation
    val filters: AbiFiltersSpec

    /**
     * Configures the [filters] with the provided configuration.
     */
    @ExperimentalAbiValidation
    fun filters(action: Action<AbiFiltersSpec>) {
        action.execute(filters)
    }

    /**
     * Provides configuration for dumps stored in the old format that are used separately in the [Binary Compatibility validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
     *
     * Use this property for a smooth migration from the old to the new dump format.
     */
    @ExperimentalAbiValidation
    val legacyDump: AbiValidationLegacyDumpExtension

    /**
     * Configures the [legacyDump] with the provided configuration.
     */
    @ExperimentalAbiValidation
    fun legacyDump(action: Action<AbiValidationLegacyDumpExtension>) {
        action.execute(legacyDump)
    }

    /**
     * The constants for ABI validation variants.
     */
    @ExperimentalAbiValidation
    companion object {
        /**
         * The report variant name for the variant configured in the `kotlin {}` block:
         *
         * ```kotlin
         * kotlin {
         *     abiValidation {
         *         // main variant
         *     }
         * }
         * ```

         *
         * This variant is also called the "main variant".
         *
         * See [AbiValidationVariantSpec] for more details about report variants.
         */
        @ExperimentalAbiValidation
        const val MAIN_VARIANT_NAME = "main"
    }
}
