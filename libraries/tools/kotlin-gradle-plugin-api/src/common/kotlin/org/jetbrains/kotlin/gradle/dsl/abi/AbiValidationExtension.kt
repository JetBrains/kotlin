/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
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
 * @since 2.2.0
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiValidationExtension {
    /**
     * @deprecated Property was removed, to enable ABI validation call function `abiValidation()`, `abiValidation { ... }` or read `abiValidation` property.
     */
    @Deprecated(
        "Property was removed, to enable ABI validation call function abiValidation(), abiValidation { ... } or read abiValidation property.",
        level = DeprecationLevel.ERROR
    )
    val enabled: Property<Boolean>
        get() = error("Property 'enabled' was removed, to enable ABI validation call function abiValidation(), abiValidation { ... } or read abiValidation property.")

    /**
     * A set of filtering rules that restrict Application Binary Interface (ABI) declarations from being included in a dump.
     *
     * The rules combine inclusion and exclusion of declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.byNames]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]):
     *
     * ```kotlin
     * abiValidation {
     *     filters {
     *         exclude {
     *             byNames.add("foo.Bar")
     *             annotatedWith.add("foo.ExcludeAbi")
     *         }
     *
     *         include {
     *             byNames.add("foo.api.**")
     *             annotatedWith.add("foo.PublicApi")
     *         }
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property or function) to get into the dump, it must pass the inclusion **and** exclusion filters.
     *
     * A declaration successfully passes the exclusion filter if it does not match any of the class name (see [AbiFilterSetSpec.byNames]) or annotation (see [AbiFilterSetSpec.annotatedWith]) filter rules.
     *
     * A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
     */
    val filters: AbiFiltersSpec

    /**
     * Configures the [filters] with the provided configuration.
     */
    fun filters(action: Action<AbiFiltersSpec>) {
        action.execute(filters)
    }

    /**
     * The directory containing reference dumps that the dump generated from the current code is compared with by the [checkTaskProvider] task.
     *
     * @since 2.4.0
     */
    val referenceDumpDir: DirectoryProperty

    /**
     * A provider for the task that compares actual dumps from the current with dumps from [referenceDumpDir].
     *
     * This task fails if any differences are found between the files.
     *
     * @since 2.4.0
     */
    val checkTaskProvider: TaskProvider<Task>

    /**
     * Overwrite dumps in the [referenceDumpDir] directory with the actual dumps for the current code.
     *
     * @since 2.4.0
     */
    val updateTaskProvider: TaskProvider<Task>

    /**
     * Whether to include the declarations for targets which are not supported by the host in the generated dump.
     * Targets which are not supported by the host in two cases:
     * - cross-compilation is disabled and some targets can't be compiled on the host machine
     * - c-interop being used and some targets can't be compiled on the host machine
     *
     * These declarations are taken from the reference dump, if available.
     *
     * If possible, unsupported targets are supplemented with common declarations that are already present in the supported targets.
     *
     * However, this does not provide a complete guarantee, so it should be used with caution.
     * This mode is intended to improve the "local" development experience only, and it is crucial to double-check ABI dumps on a host supporting corresponding compilation targets.
     *
     * If the option is set to `false` and the compiler does not support some of the Kotlin targets used in the current project, the dump generation fails with an error.
     *
     * #### Example
     *
     *  There are two targets in the project `iosX64`, `androidNativeX64` and `linuxX64`.
     *  Current dump contains class `my.Utils` which present in all targets, `my.IosUtils` only in iosX64.
     *
     *  Suppose we make such changes:
     *  - added `my.Utils2` in all targets
     *  - added `my.LinuxUtils` in linuxX64
     *  - added `my.NonAppleUtils` in linuxX64 and androidNativeX64
     *  - added `my.IosUtils2` in iosX64
     *
     *  On a host that lacks iosX64 support, and this mode is enabled, there will be such changes in the dump:
     *  - `my.Utils` will stay present in all targets of the new dump (correct inference)
     *  - `my.IosUtils` will stay present in iosX64 target of the new dump (correct inference)
     *  - `my.Utils2` will be present in all targets of the new dump (correct inference)
     *  - `my.LinuxUtils` will be present in linuxX64 target of the new dump (correct inference)
     *  - `my.NonAppleUtils` will be present in all targets of the new dump (incorrect inference - because usually if it is added to everything, then there is a high chance that the symbol is added to an unsupported target)
     *  - `my.IosUtils2` won't be present in iosX64 targets of the new dump (incorrect inference - because we can't compile the target and see what appeared individually in it)
     *
     *
     * Default value: `true`
     *
     * @since 2.4.0
     */
    val keepLocallyUnsupportedTargets: Property<Boolean>

    /**
     * Specifies the source of the binaries to extract ABI declarations from.
     *
     * Refer to the [BinariesSource] for more information.
     *
     * The default value is [BinariesSource.MAIN_COMPILATION].
     *
     * @since 2.4.0
     */
    val binariesSource: Property<BinariesSource>

    /**
     * Provides configuration for dumps stored in the old format that are used separately in the [Binary Compatibility validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
     *
     * Use this property for a smooth migration from the old to the new dump format.
     *
     * @deprecated A separate property 'legacyDump' was removed. Please place all its properties on a higher level.
     */
    @Deprecated(
        "A separate property 'legacyDump' was removed. Please place all its properties on a higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val legacyDump: AbiValidationLegacyDumpExtension
        get() = error("A separate property 'legacyDump' was removed. Please place all its properties on a higher level.")

    /**
     * Configures the [legacyDump] with the provided configuration.
     * @deprecated A separate block 'legacyDump' was removed. Please place all its properties on a higher level.
     */
    @Deprecated(
        "A separate block 'legacyDump' was removed. Please place all its properties on a higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    fun legacyDump(action: Action<AbiValidationLegacyDumpExtension>) {
        error("A separate block 'legacyDump' was removed. Please place all its properties on a higher level.")
    }

    /**
     * @deprecated Property was renamed to 'internalDumpTaskProvider'.
     */
    @Deprecated(
        "Property was renamed to 'internalDumpTaskProvider'.",
        replaceWith = ReplaceWith("internalDumpTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val legacyDumpTaskProvider: TaskProvider<org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiDumpTask>
        get() = error("Property was renamed to 'internalDumpTaskProvider'.")

    /**
     * @deprecated Property was renamed to 'checkTaskProvider'.
     */
    @Deprecated(
        "Property was renamed to 'checkTaskProvider'.",
        replaceWith = ReplaceWith("checkTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val legacyCheckTaskProvider: TaskProvider<org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiCheckTask>
        get() = error("Property was renamed to 'checkTaskProvider'.")

    /**
     * @deprecated Property was renamed to 'updateTaskProvider'.
     */
    @Deprecated(
        "Property was renamed to 'updateTaskProvider'.",
        replaceWith = ReplaceWith("updateTaskProvider"),
        level = DeprecationLevel.ERROR
    )
    val legacyUpdateTaskProvider: TaskProvider<Task>
        get() = error("Property was renamed to 'updateTaskProvider'.")

    /**
     * @deprecated Variants DSL was removed and is no longer supported.
     */
    @Deprecated("Variants DSL was removed and is no longer supported.", level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    val variants: NamedDomainObjectContainer<AbiValidationVariantSpec>
        get() = error("Variants DSL was removed and is no longer supported.")

    /**
     * @deprecated Property 'klib' was removed. Nested property 'enabled' was removed - ABI dumps always generated for klib-based targets. 'keepUnsupportedTargets' was moved to the higher level.
     */
    @Deprecated(
        "Property 'klib' was removed.\n\tNested property 'enabled' was removed - ABI dumps always generated for klib-based targets.\n\t'keepUnsupportedTargets' was moved to the higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    val klib: AbiValidationKlibKindExtension
        get() = error("Property 'klib' was removed.\n\tNested property 'enabled' was removed - ABI dumps always generated for klib-based targets.\n\t'keepUnsupportedTargets' was moved to the higher level.")

    /**
     * @deprecated Block 'klib' was removed. Nested property 'enabled' was removed - ABI dumps always generated for klib-based targets. 'keepUnsupportedTargets' was moved to the higher level.
     */
    @Deprecated(
        "Block 'klib' was removed.\n\tNested property 'enabled' was removed - ABI dumps always generated for klib-based targets.\n\t'keepUnsupportedTargets' was moved to the higher level.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DEPRECATION_ERROR")
    fun klib(action: Action<AbiValidationKlibKindExtension>) {
        error("Block 'klib' was removed.\n\tNested property 'enabled' was removed - ABI dumps always generated for klib-based targets.\n\t'keepUnsupportedTargets' was moved to the higher level.")
    }
}

/**
 * @deprecated The class 'AbiValidationVariantSpec' was removed.
 */
@KotlinGradlePluginDsl
@Deprecated("The class 'AbiValidationVariantSpec' was removed.", level = DeprecationLevel.ERROR)
@ExperimentalAbiValidation
interface AbiValidationVariantSpec {
    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    val filters: AbiFiltersSpec

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    fun filters(action: Action<AbiFiltersSpec>) {
        action.execute(filters)
    }

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    @Suppress("DEPRECATION_ERROR")
    val legacyDump: AbiValidationLegacyDumpExtension

    /**
     * Left for source compatibility.
     * The @Deprecated annotation is not needed because there is no way to use it from the script.
     */
    @Suppress("DEPRECATION_ERROR")
    fun legacyDump(action: Action<AbiValidationLegacyDumpExtension>) {
        action.execute(legacyDump)
    }

    /**
     * The constants for ABI validation variants.
     */
    companion object {
        /**
         * @deprecated Variants DSL was removed and is no longer supported.
         */
        @Deprecated("Variants DSL was removed and is no longer supported.", level = DeprecationLevel.ERROR)
        const val MAIN_VARIANT_NAME = "main"
    }
}

/**
 * Type of the source of the binaries to extract ABI declarations from.
 */
enum class BinariesSource {
    /**
     * The binaries are taken from the Maven publications of the project.
     * To do this, the `maven-publish` plugin must be applied, the Maven publications must be created and configured correctly.
     *
     * Only publishable artifacts for which no classifier is specified (is null) are taken into account.
     */
    MAVEN_PUBLICATIONS,

    /**
     * The binaries are taken from the output of the Kotlin compilation task for main and commonMain source sets.
     */
    MAIN_COMPILATION,

    /**
     * The binaries are taken from the output of the Kotlin compilation tasks which do not contain the word `test` in the name in any case.
     */
    NON_TEST_COMPILATIONS
}
