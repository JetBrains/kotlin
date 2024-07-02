/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.RUN_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

class D8EnvironmentConfigurator(subTarget: KotlinJsIrSubTarget) :
    JsEnvironmentConfigurator<D8Exec>(subTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<D8Exec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            RUN_TASK_NAME
        )
        val locateTask = project.locateTask<D8Exec>(binaryRunName)
        if (locateTask != null) return locateTask

        return D8Exec.create(binary.compilation, binaryRunName) {
            group = subTarget.taskGroupName
            dependsOn(project.tasks.named(subTarget.binarySyncTaskName(binary)))
            val inputFile = project.objects.fileProperty().value(
                binary.mainFileSyncPath
            )
            inputFileProperty.set(
                inputFile
            )
            runTaskConfigurations.all {
                it.execute(this)
            }
        }
    }
}