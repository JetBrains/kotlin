/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*

fun runTaskIrs(mainClass: String, classPath: BuildSystemIR? = null) = buildList<BuildSystemIR> {
    +ApplicationPluginIR(mainClass)

    +GradleSectionIR("application", buildBody {
        +GradleAssignmentIR("mainClassName", GradleStringConstIR(mainClass))
    })

    if (classPath != null) {
        +GetGradleTaskIR(
            name = "run",
            taskClass = "JavaExec",
            body = buildBody {
                classPath.let { +GradleAssignmentIR("classpath", it) }
            }
        )
    }
}

