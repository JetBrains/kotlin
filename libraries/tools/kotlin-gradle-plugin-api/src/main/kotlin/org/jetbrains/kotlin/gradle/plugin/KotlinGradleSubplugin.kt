/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import java.io.File

open class SubpluginOption(val key: String, private val lazyValue: Lazy<String>) {
    constructor(key: String, value: String) : this(key, lazyOf(value))

    val value: String get() = lazyValue.value
}

class FilesSubpluginOption(
    key: String,
    val files: Iterable<File>,
    val kind: FilesOptionKind = FilesOptionKind.INTERNAL,
    lazyValue: Lazy<String> = lazy { files.joinToString(File.pathSeparator) { it.canonicalPath } }
) : SubpluginOption(key, lazyValue) {

    constructor(
        key: String,
        files: List<File>,
        kind: FilesOptionKind = FilesOptionKind.INTERNAL,
        value: String? = null
    ) : this(key, files, kind, lazy { value ?: files.joinToString(File.pathSeparator) { it.canonicalPath } })
}

class CompositeSubpluginOption(
    key: String,
    lazyValue: Lazy<String>,
    val originalOptions: List<SubpluginOption>
) : SubpluginOption(key, lazyValue) {
    constructor(key: String, value: String, originalOptions: List<SubpluginOption>) : this(key, lazyOf(value), originalOptions)
}

/** Defines how the files option should be handled with regard to Gradle model */
enum class FilesOptionKind {
    /** The files option is an implementation detail and should not be treated as an input or an output.  */
    INTERNAL

    // More options might be added when use cases appear for them,
    // such as output directories, inputs or classpath options.
}

/** Defines a subplugin option that should be excluded from Gradle input/output checks */
open class InternalSubpluginOption(key: String, value: String) : SubpluginOption(key, value)

// Deprecated because most calls require the tasks to be instantiated, which is not compatible with Gradle task configuration avoidance.
@Deprecated(
    message = "This interface will be removed due to performance considerations. " +
            "Please use the KotlinCompilerPluginSupportPlugin interface instead " +
            "and remove the META-INF/services/org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin entry.",
    replaceWith = ReplaceWith("KotlinCompilerPluginSupportPlugin"),
    level = DeprecationLevel.ERROR
)
interface KotlinGradleSubplugin<in KotlinCompile : AbstractCompile> {
    fun isApplicable(project: Project, task: AbstractCompile): Boolean

    fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption>

    fun getSubpluginKotlinTasks(
        project: Project,
        kotlinCompile: KotlinCompile
    ): List<AbstractCompile> = emptyList()

    fun getCompilerPluginId(): String

    fun getPluginArtifact(): SubpluginArtifact
    fun getNativeCompilerPluginArtifact(): SubpluginArtifact? = null
}

/**
 * Gradle plugin implementing support for a Kotlin compiler plugin.
 *
 * In order to be discovered, it should be applied to the project as an ordinary Gradle [Plugin] before the
 * Kotlin plugin inspects the project model in an afterEvaluate handler.
 *
 * The default implementation of [apply]
 * doesn't do anything, but it can be overridden.
 *
 * Then its [isApplicable] is checked against compilations of the project, and if it returns true,
 * then [applyToCompilation] may be called later.
 */
interface KotlinCompilerPluginSupportPlugin : Plugin<Project> {
    override fun apply(target: Project) = Unit

    fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean

    /**
     * Configures the compiler plugin to be incorporated into a specific [kotlinCompilation].
     * This function is only called on [kotlinCompilation]s approved by [isApplicable].
     * The [Provider] returned from this function may never get queried if the compilation is avoided in the current build.
     */
    fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>>

    fun getCompilerPluginId(): String
    fun getPluginArtifact(): SubpluginArtifact

    /**
     * Kotlin/Native-specific plugin artifact.
     *
     * If Gradle is configured to use Kotlin/Native embeddable compiler jar
     * (with `kotlin.native.useEmbeddableCompilerJar=true` project property),
     * then [getPluginArtifact] is used instead.
     */
    fun getPluginArtifactForNative(): SubpluginArtifact? = null
}

open class SubpluginArtifact(val groupId: String, val artifactId: String, val version: String? = null)

class JetBrainsSubpluginArtifact(artifactId: String) : SubpluginArtifact(groupId = "org.jetbrains.kotlin", artifactId = artifactId)
