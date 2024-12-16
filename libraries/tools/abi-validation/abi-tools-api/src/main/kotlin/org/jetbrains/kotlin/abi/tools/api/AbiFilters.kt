/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

/**
 * Set of filtering rules that restrict ABI declarations included into a dump.
 *
 * It consists of a combination of rules for including and excluding declarations.
 * Each filter can be written as a filter for the class name (see [includedClasses] or [excludedClasses]), or an annotation filter (see [includedAnnotatedWith] or [excludedAnnotatedWith]).
 *
 * In order for a declaration (class, field, property or function) to get into the dump, it must pass the inclusion **and** exclusion filters.
 *
 * Declaration passes the exclusion filter if it does not match any of class name (see [excludedClasses]) or annotation  (see [excludedAnnotatedWith]) filter rule.
 *
 * Declaration passes the inclusion filters if there is no inclusion rules, or it matches any inclusion rule, or at least one of its members (actual for class declaration) matches inclusion rule.
 *
 * @since 2.1.20
 */
public class AbiFilters(
    /**
     * Include class into dump by its name.
     * Classes that do not match the specified names, that do not have an annotation from [includedAnnotatedWith]
     * and do not have members marked with an annotation from [includedAnnotatedWith] are excluded from the dump.
     *
     * The name filter works by comparing qualified class name with the value in the filter.
     *
     * For Kotlin classes, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * It is allowed to use name templates, for this purpose wildcards `**`, `*` and `?` are added.
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    public val includedClasses: Set<String>,

    /**
     * Excludes a class from a dump by its name.
     *
     * The name filter works by comparing qualified class name with the value in the filter.
     *
     * For Kotlin classes, fully qualified names are used.
     * It is important to keep in mind that dots are used everywhere as separators, even in the case of a nested class.
     * E.g. for qualified name `foo.bar.Container.Value`, here `Value` is a class nested in `Container`.
     *
     * For classes from Java sources, canonical names are used.
     * The main motivation is a similar approach to writing the class name - dots are used everywhere as delimiters.
     *
     * It is allowed to use name templates, for this purpose wildcards `**`, `*` and `?` are added.
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     */
    public val excludedClasses: Set<String>,

    /**
     * Includes a declaration by annotations placed on it.
     *
     * Any declaration that is not marked with one of the these annotations and does not match the [includedClasses] is excluded from the dump.
     *
     * If a class, a top-level function, a top-level property or a class member is annotated with one of specified annotations - then this declaration matches the filter.
     *
     * For exclusion filter it means that a class will be excluded from a dump, for inclusion filter it will keep a class or a member in a dump.
     *
     * It is allowed to use name templates, for this purpose wildcards `**`, `*` and `?` are added.
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    public val includedAnnotatedWith: Set<String>,

    /**
     * Excludes a declaration by annotations placed on it.
     *
     * If a class, a top-level function, a top-level property or a class member is annotated with one of specified annotations - then this declaration matches the filter.
     *
     * For exclusion filter it means that class will be excluded from dump, for inclusion filter it will keep class or member in dump.
     *
     * It is allowed to use name templates, for this purpose wildcards `**`, `*` and `?` are added.
     * - `**` - zero or any number of characters
     * - `*` - zero or any number of characters excluding dot. Using to specify simple class name.
     * - `?` - any single character.
     *
     * The annotation should not have [Retention] equal to [AnnotationRetention.SOURCE], otherwise, filtering by it will not work.
     */
    public val excludedAnnotatedWith: Set<String>,
) {
    public companion object {
        public val EMPTY: AbiFilters = AbiFilters(emptySet(), emptySet(), emptySet(), emptySet())
    }

    public val isEmpty: Boolean =
        includedClasses.isEmpty() && excludedClasses.isEmpty() && includedAnnotatedWith.isEmpty() && excludedAnnotatedWith.isEmpty()
}
