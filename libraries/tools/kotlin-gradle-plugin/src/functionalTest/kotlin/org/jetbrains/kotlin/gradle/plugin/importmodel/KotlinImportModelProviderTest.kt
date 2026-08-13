/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinImportModelProviderTest {
    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces stable main and test JVM models`() {
        val project = buildProjectWithJvm { kotlinJvmExtension.target.compilations.create("deploy") }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

        val base = provider.baseInformation()
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(project.kotlinToolingVersion.major, base.pluginVersion.major)
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_JVM), base.capabilitiesList)

        val projectModel = provider.projectInformation()
        val mainId = compilationUnitId { value = ":|:|jvm|main" }
        val testId = compilationUnitId { value = ":|:|jvm|test" }
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectModel.id)
        assertEquals(listOf(mainId, testId), projectModel.compilationUnitIdsList)

        assertCompilationUnit(provider.compilationUnit(mainId), mainId, "main", false)
        assertCompilationUnit(provider.compilationUnit(testId), testId, "test", true)
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedIsTest: Boolean,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.name)
        assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, model.platform)
        assertEquals(expectedIsTest, model.isTest)
        assertEquals(emptyList(), model.sourceRootsList)
        assertEquals(emptyList(), model.outputsList)
    }
}
