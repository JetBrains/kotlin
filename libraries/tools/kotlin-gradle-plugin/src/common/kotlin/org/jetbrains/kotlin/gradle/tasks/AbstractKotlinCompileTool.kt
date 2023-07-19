/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentAware
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.utils.fileExtensionCasePermutations
import org.jetbrains.kotlin.gradle.utils.property
import javax.inject.Inject

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractKotlinCompileTool<T : CommonToolArguments> @Inject constructor(
    objectFactory: ObjectFactory,
) : DefaultTask(),
    KotlinCompileTool,
    KotlinCompilerArgumentsProducer,
    CompilerArgumentAware<T>,
    TaskWithLocalState {

    @Internal
    protected val sourceFileFilter = PatternSet()

    init {
        sourceFileFilter.include(
            DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS.flatMap { ext -> ext.fileExtensionCasePermutations().map { "**/*.$it" } }
        )
    }

    private val sourceFiles = objectFactory.fileCollection()

    override val sources: FileCollection = objectFactory.fileCollection()
        .from(
            { sourceFiles.asFileTree.matching(sourceFileFilter) }
        )

    override fun source(vararg sources: Any) {
        sourceFiles.from(sources)
    }

    override fun setSource(vararg sources: Any) {
        sourceFiles.from(sources)
    }

    fun disallowSourceChanges() {
        sourceFiles.disallowChanges()
    }

    @Internal
    final override fun getIncludes(): MutableSet<String> = sourceFileFilter.includes

    @Internal
    final override fun getExcludes(): MutableSet<String> = sourceFileFilter.excludes

    final override fun setIncludes(includes: Iterable<String>): PatternFilterable = also {
        sourceFileFilter.setIncludes(includes)
    }

    final override fun setExcludes(excludes: Iterable<String>): PatternFilterable = also {
        sourceFileFilter.setExcludes(excludes)
    }

    final override fun include(vararg includes: String?): PatternFilterable = also {
        sourceFileFilter.include(*includes)
    }

    final override fun include(includes: Iterable<String>): PatternFilterable = also {
        sourceFileFilter.include(includes)
    }

    final override fun include(includeSpec: Spec<FileTreeElement>): PatternFilterable = also {
        sourceFileFilter.include(includeSpec)
    }

    final override fun include(includeSpec: Closure<*>): PatternFilterable = also {
        sourceFileFilter.include(includeSpec)
    }

    final override fun exclude(vararg excludes: String?): PatternFilterable = also {
        sourceFileFilter.exclude(*excludes)
    }

    final override fun exclude(excludes: Iterable<String>): PatternFilterable = also {
        sourceFileFilter.exclude(excludes)
    }

    final override fun exclude(excludeSpec: Spec<FileTreeElement>): PatternFilterable = also {
        sourceFileFilter.exclude(excludeSpec)
    }

    final override fun exclude(excludeSpec: Closure<*>): PatternFilterable = also {
        sourceFileFilter.exclude(excludeSpec)
    }

    @get:Internal
    final override val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    /**
     * By default, should be set by plugin from [COMPILER_CLASSPATH_CONFIGURATION_NAME] configuration.
     *
     * Empty classpath will fail the build.
     */
    @get:Classpath
    internal val defaultCompilerClasspath: ConfigurableFileCollection =
        project.objects.fileCollection()

    @get:Internal
    internal abstract val runViaBuildToolsApi: Property<Boolean>

    protected fun validateCompilerClasspath() {
        // Note that the check triggers configuration resolution
        require(!defaultCompilerClasspath.isEmpty) {
            "Default Kotlin compiler classpath is empty! Task: $path (${this::class.qualifiedName})"
        }
    }
}