/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.createCompilationDatabasesFromCompileToBitcodeTasks
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.SanitizerKind
import org.jetbrains.kotlin.konan.target.supportedSanitizers
import java.io.File
import javax.inject.Inject

/**
 * A plugin creating extensions to compile
 */
open class CompileToBitcodePlugin: Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.create(EXTENSION_NAME, CompileToBitcodeExtension::class.java, target)

        afterEvaluate {
            // TODO: Support providers (https://docs.gradle.org/current/userguide/lazy_configuration.html)
            //       in database tasks and create them along with corresponding compile tasks (not in afterEvaluate).
            createCompilationDatabasesFromCompileToBitcodeTasks(project, COMPILATION_DATABASE_TASK_NAME)
        }
    }

    companion object {
        const val EXTENSION_NAME = "bitcode"
        const val COMPILATION_DATABASE_TASK_NAME = "CompilationDatabase"
    }
}

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {

    private val targetList = with(project) {
        provider { (rootProject.property("targetList") as? List<*>)?.filterIsInstance<String>() ?: emptyList() } // TODO: Can we make it better?
    }

    fun create(
            name: String,
            srcDir: File = project.file("src/$name"),
            outputGroup: String = "main",
            configurationBlock: CompileToBitcode.() -> Unit = {}
    ) {
        targetList.get().forEach { targetName ->
            val platformManager = project.rootProject.findProperty("platformManager") as PlatformManager
            val target = platformManager.targetByName(targetName)
            val sanitizers: List<SanitizerKind?> = target.supportedSanitizers() + listOf(null)
            sanitizers.forEach { sanitizer ->
                project.tasks.register(
                        "${targetName}${name.snakeCaseToCamelCase().capitalize()}${suffixForSanitizer(sanitizer)}",
                        CompileToBitcode::class.java,
                        srcDir, name, targetName, outputGroup
                ).configure {
                    it.sanitizer = sanitizer
                    it.group = BasePlugin.BUILD_GROUP
                    val sanitizerDescription = when (sanitizer) {
                        null -> ""
                        SanitizerKind.ADDRESS -> " with ASAN"
                        SanitizerKind.THREAD -> " with TSAN"
                    }
                    it.description = "Compiles '$name' to bitcode for $targetName$sanitizerDescription"
                    it.configurationBlock()
                }
            }
        }
    }

    companion object {

        private fun String.snakeCaseToCamelCase() =
                split('_').joinToString(separator = "") { it.capitalize() }

        fun suffixForSanitizer(sanitizer: SanitizerKind?) =
            when (sanitizer) {
                null -> ""
                SanitizerKind.ADDRESS -> "_ASAN"
                SanitizerKind.THREAD -> "_TSAN"
            }

    }
}
