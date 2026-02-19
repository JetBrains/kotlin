/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl

/**
 *  A set of filtering rules that restrict Application Binary Interface (ABI) declarations from being included in a dump.
 *
 * The rules combine inclusion and exclusion of declarations.
 * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.byNames]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
 *
 * ```kotlin
 * filters {
 *     exclude {
 *         byNames.add("foo.Bar")
 *         annotatedWith.add("foo.ExcludeAbi")
 *     }
 *
 *     include {
 *         byNames.add("foo.api.**")
 *         annotatedWith.add("foo.PublicApi")
 *     }
 * }
 * ```
 *
 * In order for a declaration (class, field, property, or function) to be included in the dump, it must pass **all** inclusion and exclusion filters.
 *
 * A declaration successfully passes the exclusion filter if it does not match any of the class name (see [AbiFilterSetSpec.byNames]) or annotation  (see [AbiFilterSetSpec.annotatedWith]) filter rules.
 *
 * A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
 *
 * @since 2.2.0
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiFiltersSpec {
    /**
     *  A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * The rules combine inclusion and exclusion of declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.byNames]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
     *
     * ```kotlin
     * filters {
     *     exclude {
     *         byNames.add("foo.Bar")
     *         annotatedWith.add("foo.ExcludeAbi")
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property, or function) to be included in the dump, it must pass **all** inclusion and exclusion filters.
     *
     * @since 2.3.20
     */
    val exclude: AbiFilterSetSpec

    /**
     * A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * See [exclude] for details.
     * @deprecated Use [exclude] instead.
     */
    @Deprecated("Use 'exclude' instead", ReplaceWith("exclude"), DeprecationLevel.ERROR)
    val excluded: AbiFilterSetSpec
        get() = exclude

    /**
     * A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * It consists of a combination of rules for including declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.byNames]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
     *
     * ```kotlin
     * filters {
     *     include {
     *         byNames.add("foo.api.**")
     *         annotatedWith.add("foo.PublicApi")
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property, or function) to be included in the dump, it must pass the inclusion filter. A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
     *
     * @since 2.3.20
     */
    val include: AbiFilterSetSpec

    /**
     * A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * See [include] for details.
     * @deprecated Use [include] instead.
     */
    @Deprecated("Use 'include' instead", ReplaceWith("include"), DeprecationLevel.ERROR)
    val included: AbiFilterSetSpec
        get() = include

    /**
     * Configures the [exclude] variable with the provided configuration.
     *
     * @since 2.3.20
     */
    fun exclude(action: Action<AbiFilterSetSpec>) {
        action.execute(exclude)
    }

    /**
     * Configures the [exclude] variable with the provided configuration.
     *
     * @since 2.3.20
     */
    fun exclude(action: AbiFilterSetSpec.() -> Unit) {
        action(exclude)
    }

    /**
     * Configures the [exclude] variable with the provided configuration.
     *
     * @deprecated Use 'exclude' instead.
     */
    @Deprecated("Use 'exclude' instead", ReplaceWith("exclude"), DeprecationLevel.ERROR)
    fun excluded(action: Action<AbiFilterSetSpec>) = exclude(action)

    /**
     * Configures the [exclude] variable with the provided configuration.
     *
     * @deprecated Use 'exclude' instead.
     */
    @Deprecated("Use 'exclude' instead", ReplaceWith("exclude"), DeprecationLevel.ERROR)
    fun excluded(action: AbiFilterSetSpec.() -> Unit) = exclude(action)

    /**
     * Configures the [include] variable with the provided configuration.
     *
     * @since 2.3.20
     */
    fun include(action: Action<AbiFilterSetSpec>) {
        action.execute(include)
    }

    /**
     * Configures the [included] variable with the provided configuration.
     *
     * @since 2.3.20
     */
    fun include(action: AbiFilterSetSpec.() -> Unit) {
        action(include)
    }

    /**
     * Configures the [include] variable with the provided configuration.
     *
     * @deprecated Use 'include' instead.
     */
    @Deprecated("Use 'include' instead", ReplaceWith("include"), DeprecationLevel.ERROR)
    fun included(action: AbiFilterSetSpec.() -> Unit) = include(action)

    /**
     * Configures the [include] variable with the provided configuration.
     *
     * @deprecated Use 'include' instead.
     */
    @Deprecated("Use 'include' instead", ReplaceWith("include"), DeprecationLevel.ERROR)
    fun included(action: Action<AbiFilterSetSpec>) = include(action)
}

/**
 * A set of filters in a single direction: inclusion or exclusion.
 *
 * Inclusion filters:
 *
 * ```kotlin
 * filters {
 *     include {
 *         byNames.add("foo.api.**")
 *         annotatedWith.add("foo.PublicApi")
 *     }
 * }
 *
 * Exclusion filters:
 *
 * ```kotlin
 * filters {
 *     exclude {
 *         byNames.add("foo.Bar")
 *         annotatedWith.add("foo.ExcludeAbi")
 *     }
 * }
 * ```
 *
 * @since 2.2.0
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiFilterSetSpec {
    /**
     * Filter by a name.
     *
     * The name filter compares the symbol qualified name with the value in the filter:
     *
     * ```kotlin
     * filters {
     *     exclude {
     *         byNames.add("foo.Bar") // name filter, excludes class with name `foo.Bar` from dump
     *     }
     * }
     * ```
     *
     * For Kotlin classes, the fully qualified names are used.
     * It's important to keep in mind that periods `.` are used everywhere as separators, even in the case of nested classes.
     * For example, in the qualified name `foo.bar.Container.Value`, `Value` is a class nested inside `Container`.
     *
     * For Kotlin top-level function or properties in KLib, the fully qualified names are used.
     * It's important to keep in mind that periods `.` are used everywhere as separators, including separating package and function names.
     * For example, `foo.bar.myFunction`.
     * There are no top-level methods in Java, they are all members of some class and the class name is still necessary to filter it out.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is to ensure a consistent approach to writing class names, using periods `.` as delimiters throughout.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - Matches zero or more characters, including periods.
     * - `*` - Matches zero or more characters excluding periods. Use this to specify a single class name.
     * - `?` - Matches exactly one character.
     *
     * ```kotlin
     * filters {
     *     exclude {
     *         byNames.add("**.My*") //  A name filter that excludes classes in any non-root package with a name starting with `My`.
     *     }
     * }
     * ```
     */
    val byNames: SetProperty<String>

    /**
     * Filter by annotations placed on the declaration.
     *
     * If a class, top-level function or property, or class member (a property or a function) is annotated with one of the specified annotations, then this declaration matches the filter.
     *
     * For exclusions, matching classes, top-level symbols, or members are excluded from the dump. For inclusions, matching classes or members are included in the dump.
     *
     * For annotations from Java sources, canonical names are used.
     * The main motivation is to ensure a consistent approach to writing class names, using periods `.` as delimiters throughout.
     *
     * Name templates are allowed, with support for wildcards such as `**`, `*`, and `?`:
     * - `**` - Matches zero or more characters, including periods.
     * - `*` - Matches zero or more characters excluding periods. Use this to specify a single class name.
     * - `?` - Matches exactly one character.
     *
     * Example:
     *
     * ```kotlin
     * filters {
     *     exclude {
     *         annotatedWith.add("foo.ExcludeAbi") // exclude any class property or function annotated with 'foo.ExcludeAbi'
     *     }
     * }
     * ```
     *
     * **Important**:
     * The annotation **must** have a [Retention] of [BINARY][AnnotationRetention.BINARY] or [RUNTIME][AnnotationRetention.RUNTIME] type.
     * Annotations with [SOURCE][AnnotationRetention.SOURCE] retention cannot be analyzed and will be ignored.
     *
     */
    val annotatedWith: SetProperty<String>
}
