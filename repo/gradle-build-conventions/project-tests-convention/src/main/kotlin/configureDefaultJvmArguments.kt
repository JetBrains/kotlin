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
        val testCompilerRuntimeProvider = objects.newInstance<TestCompilerRuntimeArgumentProvider>().apply {
            testDataMap.set(extension.testDataMap)
            testDataFiles.set(extension.testDataFiles)
        }
        val javaModuleAddOpensProvider = objects.newInstance<JavaModuleAddOpensArgumentProvider>().apply {
            javaLauncher.set(testTask.javaLauncher)
        }
        jvmArgumentProviders.addAll(listOf(testCompilerRuntimeProvider, javaModuleAddOpensProvider))
    }
}
