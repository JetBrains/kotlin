/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi
abstract class K2MultiplatformStructure {

    @InternalKotlinGradlePluginApi
    data class RefinesEdge(
        @Input
        val fromFragmentName: String,
        @Input
        val toFragmentName: String,
    )

    @InternalKotlinGradlePluginApi
    data class Fragment(
        @Input
        val fragmentName: String,

        @get:InputFiles
        @get:IgnoreEmptyDirectories
        @get:Incremental
        @get:NormalizeLineEndings
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val sources: FileCollection,
    )

    @get:Nested
    abstract val refinesEdges: SetProperty<RefinesEdge>

    @get:Nested
    abstract val fragments: ListProperty<Fragment>
}

internal val K2MultiplatformStructure.fragmentsCompilerArgs: Array<String>
    get() = fragments.get().map { it.fragmentName }.toSet().toTypedArray()

internal fun K2MultiplatformStructure.fragmentSourcesCompilerArgs(sourceFileFilter: PatternFilterable? = null): Array<String> =
    fragments.get().flatMap { sourceSet ->
        sourceSet.sources
            .run { if (sourceFileFilter != null) asFileTree.matching(sourceFileFilter) else this }
            .files.map { sourceFile -> "${sourceSet.fragmentName}:${sourceFile.absolutePath}" }
    }.toTypedArray()

internal val K2MultiplatformStructure.fragmentRefinesCompilerArgs: Array<String>
    get() = refinesEdges.get().map { edge ->
        "${edge.fromFragmentName}:${edge.toFragmentName}"
    }.toTypedArray()

