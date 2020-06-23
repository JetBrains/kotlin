/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

private fun getDefaultSourceFile(f: Family): SourceFile = when (f) {
    Iterables, Collections, Lists -> SourceFile.Collections
    Sequences -> SourceFile.Sequences
    Sets -> SourceFile.Sets
    Ranges, RangesOfPrimitives, ProgressionsOfPrimitives -> SourceFile.Ranges
    ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives -> SourceFile.Arrays
    ArraysOfUnsigned -> SourceFile.UArrays
    Maps -> SourceFile.Maps
    Strings -> SourceFile.Strings
    CharSequences -> SourceFile.Strings
    Primitives -> SourceFile.Primitives
    Unsigned -> SourceFile.Unsigned
    Generic -> SourceFile.Misc
}

@TemplateDsl
abstract class TemplateBuilderBase(
    val allowedPlatforms: Set<Platform>,
    val target: KotlinTarget,
    var family: Family,
    var primitive: PrimitiveType? = null,
    var sourceFile: SourceFile = getDefaultSourceFile(family)
) {
    lateinit var signature: String   // name and params

    var sortingSignature: String? = null
        get() = field ?: signature
        private set

    val f get() = family

    val annotations: MutableList<String> = mutableListOf()
    val suppressions: MutableList<String> = mutableListOf()

    fun sourceFile(file: SourceFile) { sourceFile = file }

    fun signature(value: String, notForSorting: Boolean = false) {
        if (notForSorting) sortingSignature = signature
        signature = value
    }

    fun annotation(annotation: String) {
        annotations += annotation
    }

    fun suppress(diagnostic: String) {
        suppressions += diagnostic
    }

    fun specialFor(f: Family, action: () -> Unit) {
        if (family == f)
            action()
    }

    fun specialFor(vararg families: Family, action: () -> Unit) {
        require(families.isNotEmpty())
        if (family in families)
            action()
    }

    abstract val hasPlatformSpecializations: Boolean
    abstract fun build(builder: Appendable)
}