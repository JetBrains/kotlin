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
 * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.classes]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
 *
 * ```kotlin
 * filters {
 *     excluded {
 *         classes.add("foo.Bar")
 *         annotatedWith.add("foo.ExcludeAbi")
 *     }
 *
 *     included {
 *         classes.add("foo.api.**")
 *         annotatedWith.add("foo.PublicApi")
 *     }
 * }
 * ```
 *
 * In order for a declaration (class, field, property or function) to be included in the dump, it must pass **all** inclusion and exclusion filters.
 *
 * A declaration successfully passes the exclusion filter if it does not match any of the class name (see [AbiFilterSetSpec.classes]) or annotation  (see [AbiFilterSetSpec.annotatedWith]) filter rules.
 *
 * A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
 *
 * @since 2.1.20
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiFiltersSpec {
    /**
     *  A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * The rules combine inclusion and exclusion of declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.classes]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
     *
     * ```kotlin
     * filters {
     *     excluded {
     *         classes.add("foo.Bar")
     *         annotatedWith.add("foo.ExcludeAbi")
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property or function) to be included in the dump, it must pass **all** inclusion and exclusion filters.
     */
    val excluded: AbiFilterSetSpec

    /**
     * A set of filtering rules that restrict ABI declarations from being included in a dump.
     *
     * It consists of a combination of rules for including declarations.
     * Each filter can be written as either a class name filter (see [AbiFilterSetSpec.classes]) or an annotation filter (see [AbiFilterSetSpec.annotatedWith]).
     *
     * ```kotlin
     * filters {
     *     included {
     *         classes.add("foo.api.**")
     *         annotatedWith.add("foo.PublicApi")
     *     }
     * }
     * ```
     *
     * In order for a declaration (class, field, property or function) to be included in the dump, it must pass the inclusion filter. A declaration successfully passes the inclusion filter if no inclusion rules exist, if it matches any inclusion rule, or if at least one of its members (relevant for class declaration) matches any inclusion rule.
     */
    val included: AbiFilterSetSpec

    /**
     * Configures the [excluded] variable with the provided configuration.
     */
    fun excluded(action: Action<AbiFilterSetSpec>) {
        action.execute(excluded)
    }

    /**
     * Configures the [excluded] variable with the provided configuration.
     */
    fun excluded(action: AbiFilterSetSpec.() -> Unit) {
        action(excluded)
    }

    /**
     * Configures the [included] variable with the provided configuration.
     */
    fun included(action: Action<AbiFilterSetSpec>) {
        action.execute(included)
    }

    /**
     * Configures the [included] variable with the provided configuration.
     */
    fun included(action: AbiFilterSetSpec.() -> Unit) {
        action(included)
    }
}

/**
 * A set of filters in a single direction: inclusion or exclusion.
 *
 * Inclusion filters:
 *
 * ```kotlin
 * filters {
 *     included {
 *         classes.add("foo.api.**")
 *         annotatedWith.add("foo.PublicApi")
 *     }
 * }
 *
 * Exclusion filters:
 *
 * ```kotlin
 * filters {
 *     excluded {
 *         classes.add("foo.Bar")
 *         annotatedWith.add("foo.ExcludeAbi")
 *     }
 * }
 * ```
 *
 * @since 2.1.20
 */
@KotlinGradlePluginDsl
@ExperimentalAbiValidation
interface AbiFilterSetSpec {
    /**
     * Filter by a class name.
     *
     * The name filter compares the qualified class name with the value in the filter:
     *
     * ```kotlin
     * filters {
     *     excluded {
     *         classes.add("foo.Bar") // name filter, excludes class with name `foo.Bar` from dump
     *     }
     * }
     * ```
     *
     * For Kotlin classes, the fully qualified names are used.
     * It's important to keep in mind that periods `.` are used everywhere as separators, even in the case of nested classes.
     * For example, in the qualified name `foo.bar.Container.Value`, `Value` is a class nested inside `Container`.
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
     *     excluded {
     *         classes.add("**.My*") //  A name filter that excludes classes in any non-root package with a name starting with `My`.
     *     }
     * }
     * ```
     */
    val classes: SetProperty<String>

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
     *     excluded {
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
