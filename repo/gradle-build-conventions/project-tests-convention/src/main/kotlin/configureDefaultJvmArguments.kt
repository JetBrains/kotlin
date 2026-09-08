/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDefaultJvmArguments() {
    val extension = extensions.getByType(ProjectTestsExtension::class.java)
    tasks.withType<Test>().configureEach {
        val testTask = this
        // Snapshot all the testdata of the project unless the task narrowed it down itself.
        // `convention` doesn't overwrite a value which was already set explicitly, so this doesn't
        // depend on whether the task has been configured before or after this action runs.
        val testDataInputs = testDataInputs()
        testDataInputs.files.convention(extension.testDataFiles)
        val testCompilerRuntimeProvider = objects.newInstance<TestCompilerRuntimeArgumentProvider>().apply {
            testDataMap.set(extension.testDataMap)
            testDataFiles.setFrom(testDataInputs.files)
        }
        val javaModuleAddOpensProvider = objects.newInstance<JavaModuleAddOpensArgumentProvider>().apply {
            javaLauncher.set(testTask.javaLauncher)
        }
        jvmArgumentProviders.addAll(listOf(testCompilerRuntimeProvider, javaModuleAddOpensProvider))
    }
}
