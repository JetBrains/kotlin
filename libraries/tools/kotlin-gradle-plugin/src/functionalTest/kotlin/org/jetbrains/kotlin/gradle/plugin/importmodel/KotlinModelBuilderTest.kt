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
    fun `returns errors until the import models are generated`() {
        val project = projectWithJvm()
        assertEquals(
            Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS,
            Result.parseFrom((builder.buildAll(KotlinGradleModel::class.java.name, project) as KotlinGradleModel).kotlinModelResult).error.errorType,
        )
        for (modelId in listOf("unknown", KotlinImportModelIds.BASE, KotlinImportModelIds.COMPILATION_UNIT)) {
            assertEquals(Error.Type.ERROR_TYPE_GENERIC_ERROR, builder.buildResult(modelId, project).error.errorType)
        }
    }

    @Test
    fun `returns the generated import models`() {
        val project = projectWithJvm()
        project.generateImportModels()

        val base = builder.buildResult(KotlinImportModelIds.BASE, project)
        val projectResult = builder.buildResult(KotlinImportModelIds.PROJECT_INFORMATION, project)
        val (mainId, testId) = projectResult.model.unpack(ProjectModel::class.java).compilationUnitIdsList
        val compilation = builder.buildResult(
            KotlinImportModelIds.COMPILATION_UNIT,
            project,
            CompilationUnitModelKt.parameters { compilationUnitId = mainId }.toByteArray(),
        )
        val compilerArguments = builder.buildResult(
            KotlinImportModelIds.COMPILER_ARGUMENTS,
            project,
            CompilerArgumentsModelKt.parameters { compilationUnitId = testId }.toByteArray(),
        )
        val dependenciesParameters = DependenciesModelKt.parameters {
            compilationUnitId = testId
            scope = DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE
            coverage = DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL
        }
        val dependencies = builder.buildResult(KotlinImportModelIds.DEPENDENCIES, project, dependenciesParameters.toByteArray())

        assertEquals(KotlinImportModelIds.BASE, base.model.unpack(BaseModel::class.java).id)
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectResult.model.unpack(ProjectModel::class.java).id)
        assertEquals(mainId, compilation.model.unpack(CompilationUnitModel::class.java).parameters.compilationUnitId)
        assertEquals(testId, compilerArguments.model.unpack(CompilerArgumentsModel::class.java).parameters.compilationUnitId)
        val dependenciesModel = dependencies.model.unpack(DependenciesModel::class.java)
        assertEquals(dependenciesParameters, dependenciesModel.parameters)
        assertEquals(
            listOf(
                DependenciesModelKt.sourceDependency {
                    kind = DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_FRIEND
                    targetCompilationUnitId = mainId
                }
            ),
            dependenciesModel.sourceDependenciesList,
        )
    }

    private fun KotlinModelBuilder.buildResult(modelId: String, project: Project, parameters: ByteArray? = null): Result {
        val request = TestModelRequest(modelId, parameters)
        return Result.parseFrom((buildAll(KotlinGradleModel::class.java.name, request, project) as KotlinGradleModel).kotlinModelResult)
    }

    private fun Project.generateImportModels() {
        (tasks.getByName(GENERATE_KOTLIN_IMPORT_MODELS_TASK_NAME) as KotlinImportModelsTask).generate()
    }

    private fun projectWithJvm(): Project = buildProjectWithJvm { }.also { it.evaluate() }
}

private class TestModelRequest(
    override var kotlinModelId: String?,
    override var kotlinModelParameters: ByteArray?,
) : ModelRequest
