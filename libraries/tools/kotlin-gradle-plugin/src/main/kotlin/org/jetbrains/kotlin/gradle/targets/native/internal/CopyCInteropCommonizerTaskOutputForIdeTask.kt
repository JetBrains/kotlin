/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import java.io.File

internal open class CopyCommonizeCInteropForIdeTask : AbstractCInteropCommonizerTask() {

    private val commonizeCInteropTask: TaskProvider<CInteropCommonizerTask>
        get() = project.commonizeCInteropTask ?: throw IllegalStateException("Missing commonizeCInteropTask")

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val cInteropCommonizerTaskOutputDirectories: Provider<Set<File>> =
        commonizeCInteropTask.map { it.allOutputDirectories }

    @get:OutputDirectory
    override val outputDirectory: File = project.rootDir.resolve(".gradle/kotlin/commonizer")
        .resolve(project.path.removePrefix(":").replace(":", "/"))

    override fun getCommonizationParameters(compilation: KotlinSharedNativeCompilation): CInteropCommonizationParameters? {
        return commonizeCInteropTask.get().getCommonizationParameters(compilation)
    }

    @TaskAction
    protected fun copy() {
        outputDirectory.mkdirs()
        for (parameters in commonizeCInteropTask.get().getCommonizationParameters()) {
            val source = commonizeCInteropTask.get().outputDirectory(parameters)
            if (!source.exists()) continue
            val target = outputDirectory(parameters)
            if (target.exists()) target.deleteRecursively()
            source.copyRecursively(target, true)
        }
    }
}
