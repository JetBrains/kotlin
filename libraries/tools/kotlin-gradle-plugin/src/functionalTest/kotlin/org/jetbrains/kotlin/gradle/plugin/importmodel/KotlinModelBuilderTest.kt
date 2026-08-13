/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.*
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinModelBuilderTest {
    private val builder = KotlinModelBuilder()

    @Test
    fun `returns requested base project and compilation models`() {
        val project = projectWithJvm()
        val base = builder.buildResult(KotlinImportModelIds.BASE, project)
        val projectResult = builder.buildResult(KotlinImportModelIds.PROJECT_INFORMATION, project)
        val compilationId = projectResult.model.unpack(ProjectModel::class.java).compilationUnitIdsList.first()
        val compilation = builder.buildResult(
            KotlinImportModelIds.COMPILATION_UNIT,
            project,
            CompilationUnitModelKt.parameters { compilationUnitId = compilationId }.toByteArray(),
        )

        assertEquals(KotlinImportModelIds.BASE, base.model.unpack(BaseModel::class.java).id)
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectResult.model.unpack(ProjectModel::class.java).id)
        assertEquals(compilationId, compilation.model.unpack(CompilationUnitModel::class.java).parameters.compilationUnitId)
    }

    @Test
    fun `returns compiler arguments for the requested compilation`() {
        val project = projectWithJvm()
        val compilationId = KotlinImportModelProvider(project).projectInformation().compilationUnitIdsList.first()
        val result = builder.buildResult(
            KotlinImportModelIds.COMPILER_ARGUMENTS,
            project,
            CompilerArgumentsModelKt.parameters { compilationUnitId = compilationId }.toByteArray(),
        )
        val model = result.model.unpack(CompilerArgumentsModel::class.java)

        assertEquals(KotlinImportModelIds.COMPILER_ARGUMENTS, model.id)
        assertEquals(compilationId, model.parameters.compilationUnitId)
    }

    @Test
    fun `reports invalid import model requests`() {
        val project = projectWithJvm()
        assertEquals(
            Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID,
            builder.buildResult("unknown", project).error.errorType,
        )
        assertEquals(
            Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS,
            builder.buildResult(KotlinImportModelIds.COMPILATION_UNIT, project).error.errorType,
        )
        assertEquals(
            Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS,
            builder.buildResult(KotlinImportModelIds.COMPILER_ARGUMENTS, project).error.errorType,
        )
    }

    private fun KotlinModelBuilder.buildResult(modelId: String, project: Project, parameters: ByteArray? = null): Result {
        val request = TestModelRequest(modelId, parameters)
        return Result.parseFrom((buildAll(KotlinGradleModel::class.java.name, request, project) as KotlinGradleModel).kotlinModelResult)
    }

    private fun projectWithJvm(): Project = buildProjectWithJvm { }.also { it.evaluate() }
}

private class TestModelRequest(
    override var kotlinModelId: String?,
    override var kotlinModelParameters: ByteArray?,
) : ModelRequest
