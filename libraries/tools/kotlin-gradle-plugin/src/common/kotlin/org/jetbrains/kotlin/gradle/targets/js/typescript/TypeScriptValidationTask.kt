/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.typescript

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinIrJsGeneratedTSValidationStrategy
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import javax.inject.Inject

@DisableCachingByDefault
abstract class TypeScriptValidationTask
@Inject
constructor(
    @Internal
    @Transient
    override val compilation: KotlinJsIrCompilation
) : DefaultTask(), RequiresNpmDependencies {
    private val npmProject = compilation.npmProject

    @get:Internal
    @Transient
    protected val nodeJs = project.rootProject.kotlinNodeJsRootExtension

    private val versions = nodeJs.versions

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = setOf(versions.typescript)

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

        val result = services.execWithProgress("typescript") {
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