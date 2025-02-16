/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.filtering

import org.jetbrains.kotlin.abi.tools.api.AbiFilters

internal sealed interface FiltersMatcher {
    val isEmpty: Boolean
    val hasClassNameFilters: Boolean
    val hasAnnotationFilters: Boolean

    fun testClass(className: String, annotations: List<String>): FilterResult
    fun testClassMember(annotationNames: List<String>): FilterResult

    fun isExcludedByName(qualifiedName: String): Boolean
    fun isExcludedByAnnotations(annotationNames: List<String>): Boolean
}

internal enum class FilterResult {
    /**
     * Symbol excluded explicitly by name or annotation.
     */
    EXCLUDED,

    /**
     * Include name or annotation filters are specified and symbol is matches.
     */
    INCLUDED,

    /**
     * Include name or annotation filters are specified but symbol is not matches.
     */
    NOT_IN_INCLUDE,

    /**
     * Include name or annotation filters are not specified and symbol is not matches any exclusion filter.
     */
    PASSED
}

internal fun compileMatcher(filters: AbiFilters): FiltersMatcher {
    val included = ComplexMatcher(filters.includedClasses.processClasses(), filters.includedAnnotatedWith.processClasses())
    val excluded = ComplexMatcher(filters.excludedClasses.processClasses(), filters.excludedAnnotatedWith.processClasses())
    return FiltersMatcherImpl(included, excluded)
}

private class FiltersMatcherImpl(private val include: ComplexMatcher, private val exclude: ComplexMatcher) : FiltersMatcher {
    override val hasClassNameFilters: Boolean = !(include.classNames.isEmpty && exclude.classNames.isEmpty)
    override val hasAnnotationFilters: Boolean = !(include.annotations.isEmpty && exclude.annotations.isEmpty)
    override val isEmpty: Boolean = !(hasClassNameFilters || hasAnnotationFilters)

    override fun testClass(className: String, annotations: List<String>): FilterResult {
        var matchInclude = false
        val emptyInclusionFilters = include.classNames.isEmpty && include.annotations.isEmpty

        if (hasClassNameFilters) {
            // exclude class if it's matches any of exclude filter
            if (!exclude.classNames.isEmpty && exclude.classNames.matches(className)) return FilterResult.EXCLUDED

            if (include.classNames.matches(className)) {
                matchInclude = true
            }
        }

        if (hasAnnotationFilters) {
            val checkExclude = !exclude.annotations.isEmpty
            val checkInclude = !include.annotations.isEmpty

            annotations.forEach { annotation ->
                if (checkExclude && exclude.annotations.matches(annotation)) return FilterResult.EXCLUDED

                if (checkInclude && !matchInclude && include.annotations.matches(annotation)) {
                    matchInclude = true
                }

                // no reason to continue check - no exclusion filters,  no inclusion filters or include already matches
                if (!checkExclude && (!checkInclude || matchInclude)) return@forEach
            }
        }

        return if (emptyInclusionFilters) {
            FilterResult.PASSED
        } else {
            if (matchInclude) {
                FilterResult.INCLUDED
            } else {
                FilterResult.NOT_IN_INCLUDE
            }
        }
    }

    override fun isExcludedByName(qualifiedName: String): Boolean {
        if (!hasClassNameFilters) return false
        // exclude class if it's matches any of exclude filter
        if (!exclude.classNames.isEmpty && exclude.classNames.matches(qualifiedName)) return true
        // exclude class if include filters are specified and it isn't matches this include filters
        if (!include.classNames.isEmpty && !include.classNames.matches(qualifiedName)) return true
        return false
    }

    override fun isExcludedByAnnotations(annotationNames: List<String>): Boolean {
        val checkExclude = !exclude.annotations.isEmpty
        var checkInclude = !include.annotations.isEmpty

        var inInclude = false
        annotationNames.forEach { annotation ->
            if (checkExclude && exclude.annotations.matches(annotation)) return true
            if (checkInclude && include.annotations.matches(annotation)) {
                checkInclude = false
                inInclude = true

                // if we match any include annotation and no exclude annotation - we may stop
                if (!checkExclude) return false
            }
        }

        return !include.annotations.isEmpty && !inInclude
    }

    override fun testClassMember(annotationNames: List<String>): FilterResult {
        val checkExclude = !exclude.annotations.isEmpty
        val checkInclude = !include.annotations.isEmpty

        // no filters
        if (!checkExclude && !checkInclude) return FilterResult.PASSED

        var matchInclude = false

        annotationNames.forEach { annotation ->
            if (checkExclude && exclude.annotations.matches(annotation)) return FilterResult.EXCLUDED
            if (checkInclude && include.annotations.matches(annotation)) {
                matchInclude = true
            }

            if (!checkExclude && matchInclude) return@forEach
        }

        return if (!checkInclude) {
            // no inclusion filters
            FilterResult.PASSED
        } else {
            if (matchInclude) {
                FilterResult.INCLUDED
            } else {
                FilterResult.NOT_IN_INCLUDE
            }
        }
    }

    companion object {
        val EMPTY =
            FiltersMatcherImpl(
                ComplexMatcher(WildcardsMatcher.EMPTY, WildcardsMatcher.EMPTY),
                ComplexMatcher(WildcardsMatcher.EMPTY, WildcardsMatcher.EMPTY)
            )
    }
}

private class ComplexMatcher(val classNames: WildcardsMatcher, val annotations: WildcardsMatcher)

private class WildcardsMatcher(
    /**
     * Full name of the class to pass filers.
     *
     * A class name passes the filter only if it matches one of the saved ones by character.
     *
     * Store values: `foo.bar.ClassName`, `RootClass`, `foo.bar.Container.Nested`
     */
    val full: Set<String>,

    /**
     * Prefix in qualified class name.
     *
     * Can contain dots and dots are acceptable after the filter.
     * It is also allowed that the name will be equal to the value of the filter.
     *
     * Corresponds to filters like `"foo.bar.**"`, `"foo.bar**"`
     *
     * Class `foo.bar.Biz` matches filters `foo.`, `foo.bar`, `foo.bar.`, `foo.bar.B` and `foo.bar.Biz`
     */
    val anyPostfix: Set<String>,

    /**
     * Prefix in qualified class name after which there is no `.` symbol.
     *
     * Can contain dots but dots are not acceptable after the filter.
     * It is also allowed that the name will be equal to the value of the filter.
     *
     * Corresponds to filters like `"foo.bar.*"`, `"foo.bar*"`
     *
     * Class `foo.bar.Biz` matches filters, `foo.bar.`, `foo.bar.B` and `foo.bar.Biz` and don't matches `foo.`, `foo.bar`
     */
    val anyRightSegment: Set<String>,

    /**
     * Postfix in qualified class name.
     *
     * Can contain dots and dots are acceptable before the filter.
     * It is also allowed that the name will be equal to the value of the filter.
     *
     * Corresponds to filters like `"**.foo.Bar"`, `"**Bar"`
     *
     * Class `foo.bar.Biz` matches filters `foo.`, `foo.bar`, `foo.bar.`, `foo.bar.B` and `foo.bar.Biz`
     */
    val anyPrefix: Set<String>,

    /**
     * Regular expressions to match qualified class names.
     *
     * It contains all the filters not listed above.
     * Like `"?foo"`, `"foo.**.Bar"`, `foo*bar`, `**.foo.**`, and its combinations.
     */
    val regex: List<Regex>,
) {

    fun matches(className: String): Boolean {
        if (isEmpty) return false

        if (full.isNotEmpty() && full.contains(className)) {
            return true
        }
        if (anyPostfix.isNotEmpty()) {
            anyPostfix.forEach { prefix ->
                if (className.startsWith(prefix)) return true
            }
        }

        if (anyRightSegment.isNotEmpty()) {
            anyRightSegment.forEach { prefix ->
                if (
                    className.startsWith(prefix)
                    // same string OR substring without dot after prefix
                    && (className.length == prefix.length || className.indexOf('.', prefix.length) < 0)
                ) {
                    return true
                }
            }
        }

        if (anyPrefix.isNotEmpty()) {
            anyPrefix.forEach { postfix ->
                if (className.endsWith(postfix)) return true
            }
        }

        if (regex.isNotEmpty()) {
            regex.forEach { regex ->
                if (regex.matches(className)) return true
            }
        }
        return false
    }

    companion object {
        val EMPTY = WildcardsMatcher(emptySet(), emptySet(), emptySet(), emptySet(), emptyList())
    }

    val isEmpty = full.isEmpty() && anyPostfix.isEmpty() && anyRightSegment.isEmpty() && anyPrefix.isEmpty() && regex.isEmpty()
}


private fun Iterable<String>.processClasses(): WildcardsMatcher {
    val full = mutableSetOf<String>()
    val anyPostfix = mutableSetOf<String>()
    val anyRightSegment = mutableSetOf<String>()
    val anyPrefix = mutableSetOf<String>()
    val regex = mutableListOf<Regex>()

    forEach { classFilter ->
        val hasQuestionWildcard = classFilter.contains('?')
        val hasStarWildcard = classFilter.contains('*')

        if (hasQuestionWildcard) {
            regex += classFilter.wildcardsToRegex()
            return@forEach
        }

        // There is no any wildcards - fully qualified class name
        if (!hasStarWildcard) {
            full += classFilter
            return@forEach
        }

        // specific class in any package
        // **a.MyClassName
        if (classFilter.startsWith("**") && classFilter.lastIndexOf('*') == 1) {
            anyPrefix += classFilter.removePrefix("**")
            return@forEach
        }

        // any class in specific package
        // com.example.*
        if (classFilter.endsWith("*") && (classFilter.indexOf('*') == classFilter.length - 1)) {
            anyRightSegment += classFilter.removeSuffix("*")
            return@forEach
        }
        // any class in specific package or its subpackage
        // com.example.**
        if (classFilter.endsWith("**") && (classFilter.indexOf('*') == classFilter.length - 2)) {
            anyPostfix += classFilter.removeSuffix("**")
            return@forEach
        }
        regex += classFilter.wildcardsToRegex()
    }
    return WildcardsMatcher(full, anyPostfix, anyRightSegment, anyPrefix, regex)
}
