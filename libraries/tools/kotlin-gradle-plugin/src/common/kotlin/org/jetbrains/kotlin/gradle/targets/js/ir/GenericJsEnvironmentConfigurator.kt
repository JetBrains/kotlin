/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

class GenericJsEnvironmentConfigurator(subTarget: KotlinJsIrSubTarget) :
    JsEnvironmentConfigurator<Exec>(subTarget) {

    override fun configureBinaryRun(binary: JsIrBinary): TaskProvider<Exec> {
        val binaryRunName = subTarget.disambiguateCamelCased(
            binary.mode.name.toLowerCaseAsciiOnly(),
            "run"
        )

        val locateTask = project.locateTask<Exec>(binaryRunName)
        if (locateTask != null) return locateTask

        return project.registerTask(binaryRunName)
    }

}
