/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.commonizer.toolsDir
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.property
import javax.inject.Inject

internal val KotlinLLDBScriptSetupAction = KotlinProjectSetupCoroutine {
    locateOrRegisterLLDBScriptTask()
}

@DisableCachingByDefault(because = "The task is just writing to the file, so no need to cache")
internal abstract class LLDBInitTask
@Inject constructor(
    objects: ObjectFactory,
    projectLayout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    internal val fileName: Property<String> = objects.property(initialValue = "lldbinit")

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val konanToolsDir: DirectoryProperty

    @get:OutputFile
    protected val outputFile: RegularFileProperty = objects.fileProperty().convention(
        projectLayout.buildDirectory.file(fileName)
    )

    @TaskAction
    fun createScript() {
        outputFile
            .getFile()
            .writeText("command script import ${konanToolsDir.getFile().resolve("konan_lldb.py")}")
    }
}

internal fun Project.locateOrRegisterLLDBScriptTask(): TaskProvider<LLDBInitTask> {
    return locateOrRegisterTask("setupLldbScript") { task ->
        task.description = "Generate lldbinit file with imported konan_lldb.py script"
        task.konanToolsDir.set(konanDistribution.toolsDir)
    }
}