/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.File

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

    /**
     * If new sources were added to the Compile Task,
     * and they weren't mapped to any of the fragments then [defaultFragmentName] will be used
     *
     * It is marked with @Optional as an extra protection measure for cases when some task extends
     * a compile task but doesn't need K2 Structure for example [KotlinJsIrLink]
     *
     * @see KotlinCompileTool.source
     */
    @get:Input
    @get:Optional
    abstract val defaultFragmentName: Property<String>
}

internal val K2MultiplatformStructure.fragmentsCompilerArgs: Array<String>
    get() = fragments.get().map { it.fragmentName }.toSet().toTypedArray()

private fun fragmentSourceCompilerArg(sourceFile: File, fragmentName: String) = "$fragmentName:${sourceFile.absolutePath}"

internal fun K2MultiplatformStructure.fragmentSourcesCompilerArgs(
    allSources: Collection<File>,
    sourceFileFilter: PatternFilterable? = null
): Array<String> {
    val sourcesWithKnownFragment = mutableSetOf<File>()
    val fragmentSourcesCompilerArgs = fragments.get().flatMap { sourceSet ->
        sourceSet.sources
            .run { if (sourceFileFilter != null) asFileTree.matching(sourceFileFilter) else this }
            .files.map { sourceFile ->
                sourcesWithKnownFragment.add(sourceFile)
                fragmentSourceCompilerArg(sourceFile, sourceSet.fragmentName)
            }
    }.toMutableList()

    val sourcesWithUnknownFragment = allSources - sourcesWithKnownFragment
    val defaultFragmentName = defaultFragmentName.orNull
    if (defaultFragmentName != null) {
        sourcesWithUnknownFragment.mapTo(fragmentSourcesCompilerArgs) { fragmentSourceCompilerArg(it, defaultFragmentName) }
    }

    return fragmentSourcesCompilerArgs.toTypedArray()
}

internal val K2MultiplatformStructure.fragmentRefinesCompilerArgs: Array<String>
    get() = refinesEdges.get().map { edge ->
        "${edge.fromFragmentName}:${edge.toFragmentName}"
    }.toTypedArray()

