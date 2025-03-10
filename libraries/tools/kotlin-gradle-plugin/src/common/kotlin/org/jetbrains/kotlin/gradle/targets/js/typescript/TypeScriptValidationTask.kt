/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.typescript

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinIrJsGeneratedTSValidationStrategy
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.utils.getExecOperations
import javax.inject.Inject

@DisableCachingByDefault
abstract class TypeScriptValidationTask
@Inject
internal constructor(
    @Internal
    @Transient
    final override val compilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
    private val execOps: ExecOperations,
) : DefaultTask(), RequiresNpmDependencies {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DEPRECATION")
    constructor(
        compilation: KotlinJsIrCompilation,
    ) : this(
        compilation = compilation,
        objects = compilation.project.objects,
        execOps = compilation.project.getExecOperations(),
    )

    private val npmProject: NpmProject = compilation.npmProject

    @get:Internal
    internal abstract val versions: Property<NpmVersions>

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = setOf(versions.get().typescript)

    @get:SkipWhenEmpty
    @get:NormalizeLineEndings
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:Input
    abstract val validationStrategy: Property<KotlinIrJsGeneratedTSValidationStrategy>

    private val generatedDts
        get() = inputDir.asFileTree.matching { it.include("*.d.ts") }.files

    @TaskAction
    fun run() {
        val validationStrategy = validationStrategy.get()

        if (validationStrategy == KotlinIrJsGeneratedTSValidationStrategy.IGNORE) return

        val files = generatedDts.map { it.absolutePath }

        if (files.isEmpty()) return

        val progressLogger = objects.newBuildOpLogger()
        val result = execWithProgress(progressLogger, "typescript", execOps) {
            npmProject.useTool(it, "typescript/bin/tsc", listOf(), listOf("--noEmit"))
        }

        if (result.exitValue == 0) return

        val message = "Oops, Kotlin/JS compiler generated invalid d.ts files."

        if (validationStrategy == KotlinIrJsGeneratedTSValidationStrategy.ERROR) {
            error(message)
        }
    }

    companion object {
        const val NAME: String = "validateGeneratedByCompilerTypeScript"
    }
}
