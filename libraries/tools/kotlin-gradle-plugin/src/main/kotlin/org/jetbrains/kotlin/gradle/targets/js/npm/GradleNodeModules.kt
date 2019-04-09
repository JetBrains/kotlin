/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.CopySpec
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.ProcessedFilesCache
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

internal class GradleNodeModules(val project: Project) : AutoCloseable {
    companion object {
        const val STATE_FILE_NAME = ".visited"
    }

    internal val dir = project.buildDir.resolve("node_modules_gradle")
    private val nodeModulesDir = project.npmProject.nodeModulesDir
    private val cache = ProcessedFilesCache(project, dir, STATE_FILE_NAME, "5")
    private val visited = mutableSetOf<ResolvedDependency>()
    private val doLast = mutableListOf<() -> Unit>()

    val modules
        get() = cache.targets.map {
            GradleNodeModule(it, dir.resolve(it))
        }

    fun visitCompilation(compilation: KotlinCompilation<KotlinCommonOptions>) {
        val project = compilation.target.project
        val kotlin2JsCompile = compilation.compileKotlinTask as Kotlin2JsCompile

        // classpath
        compilation.relatedConfigurationNames.forEach {
            val configuration = project.configurations.getByName(it)
            if (configuration.isCanBeResolved) {
                configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                    visitDependency(compilation, it)
                }
            }
        }

        // output
        doLast.add {
            // we should do it only after node package manger work (as it can clean files)
            if (kotlin2JsCompile.state.executed) {
                visitCompile(project, kotlin2JsCompile)
            } else {
                kotlin2JsCompile.doLast {
                    visitCompile(project, kotlin2JsCompile)
                }
            }
        }
    }

    private fun visitDependency(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        dependency: ResolvedDependency
    ) {
        if (!visited.add(dependency)) return

        visitArtifacts(dependency, dependency.moduleArtifacts)

        dependency.children.forEach {
            visitDependency(compilation, it)
        }
    }

    private fun visitArtifacts(
        dependency: ResolvedDependency,
        artifacts: MutableSet<ResolvedArtifact>
    ) {
        val lazyDirName: String? by lazy {
            val module = GradleNodeModuleBuilder(project, dependency, artifacts, this)
            module.visitArtifacts()
            module.rebuild()?.canonicalFile?.relativeTo(dir)?.path
        }

        artifacts.forEach { artifact ->
            cache.getOrCompute(artifact.file) {
                lazyDirName
            }
        }
    }

    private fun visitCompile(project: Project, kotlin2JsCompile: Kotlin2JsCompile) {
        project.copy { copy ->
            copy.from(kotlin2JsCompile.outputFile)
            copy.from(kotlin2JsCompile.outputFile.path + ".map")
            copy.into(nodeModulesDir)
            copy.withSourceMapRewriter()
        }
    }

    private fun CopySpec.withSourceMapRewriter() {
        eachFile {
            if (it.name.endsWith(".js.map")) {
                it.filter(
                    mapOf(
                        "srcSourceRoot" to it.file.parentFile,
                        "targetSourceRoot" to nodeModulesDir
                    ),
                    RewriteSourceMapFilterReader::class.java
                )
            }
        }
    }

    override fun close() {
        doLast.forEach { it() }
        cache.close()
    }
}