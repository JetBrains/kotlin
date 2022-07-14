/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.testDsl

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseWrapper
import org.jetbrains.kotlin.project.model.infra.*

fun KpmTestCaseWrapper.describeCase(
    applyDefaults: Boolean = true,
    configure: KpmTestCase.() -> Unit = { }
): KpmTestCase = describeCase(this::class.simpleName!!, applyDefaults, configure)

fun describeCase(name: String, applyDefaults: Boolean = true, configure: KpmTestCase.() -> Unit = { }): KpmTestCase {
    val case = KpmTestCase(name)
    if (applyDefaults) case.applyDefaults()
    case.configure()
    return case
}

fun KpmTestCase.project(
    name: String,
    applyDefaults: Boolean = true,
    configure: TestKpmModuleContainer.() -> Unit = { }
): TestKpmModuleContainer {
    val project = projects.getOrPut(name) { TestKpmModuleContainer(this, name) }
    if (applyDefaults) project.applyDefaults()
    project.configure()
    return project
}

fun KpmTestCase.projectNamed(name: String) = projects[name]
    ?: error("Project with name $name doesn't exist. Existing projects: ${projects.joinToString { it.name }}")

fun KpmTestCase.allModules(configure: TestKpmModule.() -> Unit) {
    projects.withAll {
        modules.withAll(configure)
    }
}

fun KpmTestCase.allFragments(configure: TestKpmFragment.() -> Unit) {
    projects.withAll {
        modules.withAll {
            fragments.withAll(configure)
        }
    }
}
