/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.gradle.textWithoutTags
import org.jetbrains.kotlin.idea.run.KotlinJvmTestClassGradleConfigurationProducer
import org.jetbrains.kotlin.idea.run.KotlinJvmTestMethodGradleConfigurationProducer
import org.jetbrains.kotlin.idea.run.getConfiguration
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test

class GradleTestRunConfigurationCustomTest : KotlinGradleImportingTestCase() {
    @Test
    @TargetVersions("4.7+")
    fun testPreferredConfigurations() {
        val files = importProjectFromTestData()

        runInEdtAndWait {
            runReadAction {
                val javaFile = files.first { it.name == "MyTest.java" }
                val kotlinFile = files.first { it.name == "MyKotlinTest.kt" }

                val javaClassConfiguration = getConfiguration(javaFile, myProject, "MyTest")
                assert(javaClassConfiguration.isProducedBy(TestClassGradleConfigurationProducer::class.java))
                assert(javaClassConfiguration.configuration.name == "MyTest")

                val javaMethodConfiguration = getConfiguration(javaFile, myProject, "testA")
                assert(javaMethodConfiguration.isProducedBy(TestMethodGradleConfigurationProducer::class.java))
                assert(javaMethodConfiguration.configuration.name == "MyTest.testA")

                val kotlinClassConfiguration = getConfiguration(kotlinFile, myProject, "MyKotlinTest")
                assert(kotlinClassConfiguration.isProducedBy(KotlinJvmTestClassGradleConfigurationProducer::class.java))
                assert(kotlinClassConfiguration.configuration.name == "MyKotlinTest")

                val kotlinFunctionConfiguration = getConfiguration(kotlinFile, myProject, "testA")
                assert(kotlinFunctionConfiguration.isProducedBy(KotlinJvmTestMethodGradleConfigurationProducer::class.java))
                assert(kotlinFunctionConfiguration.configuration.name == "MyKotlinTest.testA")
            }
        }
    }

    override fun testDataDirName(): String {
        return "testRunConfigurations"
    }

    override fun createProjectSubFile(relativePath: String, content: String): VirtualFile {
        val file = createProjectSubFile(relativePath)
        ExternalSystemTestCase.setFileContent(file, textWithoutTags(content), /* advanceStamps = */ false)
        return file
    }
}