/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildController
import org.gradle.util.GradleVersion
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.importmodels.proto.action as actionModel
import org.jetbrains.kotlin.importmodels.proto.ActionKt.gradleTask as gradleTaskModel
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import java.io.Serializable
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinImportModelsToolingApiIT : KGPBaseTest() {
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    @JvmGradlePluginTests
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `Tooling API returns stable basic JVM import models`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
                runViaBuildToolsApi = true,
                generateCompilerRefIndex = true,
            ),
        ) {
            buildScriptInjection {
                kotlinJvm.compilerOptions {
                    optIn.add("my.custom.OptInAnnotation")
                    freeCompilerArgs.add("-Xdebug")
                }
                val generateImportModelSources = project.tasks.register("generateImportModelSources") {
                    it.outputs.dir(project.layout.buildDirectory.dir("generated/import-models"))
                    it.doLast { error("generated-source producer must not execute during import-model retrieval") }
                }
                kotlinJvm.sourceSets.getByName("main").generatedKotlin.srcDir(generateImportModelSources)
            }
            val first = runBuildAction(KotlinImportModelsBuildAction()).toModels()
            val second = runBuildAction(KotlinImportModelsBuildAction()).toModels()
            val base = first.base.model.unpack(BaseModel::class.java)
            val project = first.project.model.unpack(ProjectModel::class.java)
            val units = first.compilationUnits.map { it.model.unpack(CompilationUnitModel::class.java) }
            val compilerArguments = first.compilerArguments.map { it.model.unpack(CompilerArgumentsModel::class.java) }
            val dependencies = first.dependencies.map { it.model.unpack(DependenciesModel::class.java) }
            val compilerPluginDependencies = first.compilerPluginDependencies.map { it.model.unpack(DependenciesModel::class.java) }
            val main = units.single { it.name == "main" }
            val test = units.single { it.name == "test" }
            val mainDependencies = dependencies.single { it.parameters.compilationUnitId == main.parameters.compilationUnitId }
            val testDependencies = dependencies.single { it.parameters.compilationUnitId == test.parameters.compilationUnitId }

            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), base.capabilitiesList)
            assertEquals(listOf("main", "test"), units.map { it.name })
            assertEquals(
                listOf(
                    listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
                    listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
                ),
                units.map(CompilationUnitModel::getTargetPlatformsList),
            )
            assertEquals(listOf("jvm", "jvm"), units.map(CompilationUnitModel::getTargetName))
            assertTrue(units.all(CompilationUnitModel::hasTargetName))
            assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, compilerArguments.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, dependencies.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, compilerPluginDependencies.map { it.parameters.compilationUnitId })
            assertTrue(compilerPluginDependencies.all { it.parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILER_PLUGIN })
            assertTrue(compilerPluginDependencies.all { it.parameters.coverage == DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL })
            assertTrue(compilerPluginDependencies.all { it.compilationRelationsList.isEmpty() })
            assertTrue(compilerArguments.all { it.id == KotlinImportModelIds.COMPILER_ARGUMENTS })
            assertTrue(dependencies.all { it.id == KotlinImportModelIds.DEPENDENCIES })
            assertTrue(dependencies.all { it.parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE })
            assertTrue(dependencies.all { it.parameters.coverage == DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL })
            assertTrue(dependencies.all { it.classpathEntriesList.isNotEmpty() })
            assertEquals(emptyList(), mainDependencies.compilationRelationsList)
            assertEquals(
                listOf(
                    DependenciesModelKt.compilationRelation {
                        kind = DependenciesModel.CompilationRelation.Kind.COMPILATION_RELATION_KIND_FRIEND
                        targetCompilationUnitId = main.parameters.compilationUnitId
                    }
                ),
                testDependencies.compilationRelationsList,
            )
            assertTrue("-Xdebug" in compilerArguments.first().argumentsList)
            assertTrue("-opt-in my.custom.OptInAnnotation" in compilerArguments.first().argumentsList.joinToString(" "))
            assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, units.first().platform)
            assertEquals(CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN, units.first().purpose)
            assertEquals(
                listOf(
                    output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin"),
                    output("build/kotlin/compileKotlin/cacheable/cri", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI, ":compileKotlin"),
                ),
                units.first().outputsList,
            )
            assertEquals(
                sourceRoots(
                    sourceRoot(
                        "build/generated/import-models",
                        SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                        ":generateImportModelSources",
                    ),
                    sourceRoot("src/main/java"),
                    sourceRoot("src/main/kotlin"),
                ),
                units.first().sourceRootsList,
            )
            assertEquals(CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST, units.last().purpose)
            assertEquals(
                listOf(
                    output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin"),
                    output(
                        "build/kotlin/compileTestKotlin/cacheable/cri",
                        CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI,
                        ":compileTestKotlin"
                    ),
                ),
                units.last().outputsList,
            )
            assertEquals(
                sourceRoots(sourceRoot("src/test/java"), sourceRoot("src/test/kotlin")),
                units.last().sourceRootsList,
            )
            assertEquals(project.compilationUnitIdsList, second.project.model.unpack(ProjectModel::class.java).compilationUnitIdsList)
            assertEquals(first.compilationUnits, second.compilationUnits)
            assertEquals(first.dependencies, second.dependencies)
            assertFalse(projectPath.resolve("build/classes/kotlin/main").exists())
            assertFalse(projectPath.resolve("build/kotlin/compileKotlin/cacheable/cri").exists())
        }
    }

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    @MppGradlePluginTests
    fun `Tooling API returns KMP import models including test compilations without materializing outputs`(gradleVersion: GradleVersion) {
        val producer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.jvm("producerJvm")
                kotlinMultiplatform.linuxX64("producerLinuxX64")
                kotlinMultiplatform.linuxArm64("producerLinuxArm64")
            }
        }
        project(
            projectName = "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
                runViaBuildToolsApi = true,
                generateCompilerRefIndex = true,
            ),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            include(producer, "producer")
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()
                kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":producer"))
                }
            }

            val models = runBuildAction(KotlinImportModelsBuildAction()).toModels()
            val base = models.base.model.unpack(BaseModel::class.java)
            val project = models.project.model.unpack(ProjectModel::class.java)
            val units = models.compilationUnits.map { it.model.unpack(CompilationUnitModel::class.java) }
            val compilerArguments = models.compilerArguments.map { it.model.unpack(CompilerArgumentsModel::class.java) }
            val dependencies = models.dependencies.map { it.model.unpack(DependenciesModel::class.java) }
            val compilerPluginDependencies = models.compilerPluginDependencies.map { it.model.unpack(DependenciesModel::class.java) }
            val metadata = units.single { it.platform == CompilationUnitModel.Platform.PLATFORM_METADATA }
            val jvm = units.single { it.platform == CompilationUnitModel.Platform.PLATFORM_JVM && it.name == "main" }
            val jvmTest = units.single { it.platform == CompilationUnitModel.Platform.PLATFORM_JVM && it.name == "test" }
            val nativeTest = units.single { it.platform == CompilationUnitModel.Platform.PLATFORM_NATIVE && it.name == "test" }
            val dependenciesByCompilationId = dependencies.associateBy { it.parameters.compilationUnitId }

            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(
                listOf(BaseModel.Capability.CAPABILITY_KOTLIN_MULTIPLATFORM),
                base.capabilitiesList,
            )
            assertEquals(
                listOf(
                    ":|:|jvm|main",
                    ":|:|jvm|test",
                    ":|:|linuxX64|main",
                    ":|:|linuxX64|test",
                    ":|:|metadata|commonMain",
                ),
                project.compilationUnitIdsList.map { it.value },
            )
            assertEquals(5, units.size)
            assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, compilerArguments.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, dependencies.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, compilerPluginDependencies.map { it.parameters.compilationUnitId })
            assertTrue(compilerPluginDependencies.all { it.parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILER_PLUGIN })
            assertTrue(compilerPluginDependencies.all { it.parameters.coverage == DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL })
            assertTrue(compilerPluginDependencies.all { it.compilationRelationsList.isEmpty() })
            assertEquals("commonMain", metadata.name)
            assertEquals(
                listOf(
                    CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM,
                    CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_NATIVE,
                ),
                metadata.targetPlatformsList,
            )
            assertFalse(metadata.hasTargetName())
            assertEquals(CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN, metadata.purpose)
            assertEquals(CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST, jvmTest.purpose)
            assertEquals("jvm", jvmTest.targetName)
            assertTrue(jvmTest.outputsList.any { it.kind == CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI })
            assertEquals(CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST, nativeTest.purpose)
            assertEquals("linuxX64", nativeTest.targetName)
            assertTrue(nativeTest.outputsList.all { it.kind == CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB })
            assertEquals(
                listOf(compilationRelation(jvm.parameters.compilationUnitId)),
                dependenciesByCompilationId.getValue(jvmTest.parameters.compilationUnitId).compilationRelationsList,
            )
            assertEquals(
                mapOf(
                    metadata.parameters.compilationUnitId to ":|:producer|metadata|commonMain",
                    jvm.parameters.compilationUnitId to ":|:producer|producerJvm|main",
                    compilationUnitId { value = ":|:|linuxX64|main" } to ":|:producer|producerLinuxX64|main",
                ),
                listOf(metadata.parameters.compilationUnitId, jvm.parameters.compilationUnitId, compilationUnitId { value = ":|:|linuxX64|main" })
                    .associateWith {
                        dependenciesByCompilationId.getValue(it).classpathEntriesList.single { entry -> entry.hasProject() }.project
                            .targetCompilationUnitId.value
                    },
            )
            assertEquals(
                mapOf(
                    metadata.parameters.compilationUnitId to projectPath.toRealPath().resolve("producer/build/libs/producer-metadata.jar").toString(),
                    jvm.parameters.compilationUnitId to projectPath.toRealPath().resolve("producer/build/libs/producer-producerjvm.jar").toString(),
                    compilationUnitId { value = ":|:|linuxX64|main" } to
                        projectPath.toRealPath().resolve("producer/build/classes/kotlin/producerLinuxX64/main/klib/producer").toString(),
                ),
                listOf(metadata.parameters.compilationUnitId, jvm.parameters.compilationUnitId, compilationUnitId { value = ":|:|linuxX64|main" })
                    .associateWith {
                        dependenciesByCompilationId.getValue(it).classpathEntriesList.single { entry -> entry.hasProject() }.project.artifactPath
                    },
            )
            assertTrue(dependencies.all { dependency ->
                val projectEntry = dependency.classpathEntriesList.singleOrNull { it.hasProject() }?.project
                projectEntry == null || dependency.classpathEntriesList.none {
                    it.hasBinary() && it.binary.artifactPath == projectEntry.artifactPath
                }
            })
            assertTrue(units.flatMap { it.outputsList }.all { output -> !projectPath.resolve(output.path).exists() })
            assertTrue(producer.projectPath.resolve("build").toFile().walkTopDown().none { it.isFile })
        }
    }

}

private fun sourceRoots(vararg roots: SourceRoot): List<SourceRoot> = roots.toList()

private fun sourceRoot(
    path: String,
    kind: SourceRoot.Kind = SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE,
    vararg producingTaskPaths: String,
): SourceRoot = sourceRootModel {
    this.path = path
    this.kind = kind
    producingActions += producingTaskPaths.map(::gradleAction)
}

private fun gradleAction(taskPath: String): Action = actionModel {
    gradleAction = gradleTaskModel { this.taskPath = taskPath }
}

private fun output(
    path: String,
    kind: CompilationUnitModel.Output.Kind,
    vararg producingTaskPaths: String,
): CompilationUnitModel.Output = CompilationUnitModelKt.output {
    this.path = path
    this.kind = kind
    producingActions += producingTaskPaths.map(::gradleAction)
}

private fun compilationRelation(targetCompilationUnitId: CompilationUnitId): DependenciesModel.CompilationRelation =
    DependenciesModelKt.compilationRelation {
        kind = DependenciesModel.CompilationRelation.Kind.COMPILATION_RELATION_KIND_FRIEND
        this.targetCompilationUnitId = targetCompilationUnitId
    }

private class KotlinImportModelsBuildAction : org.gradle.tooling.BuildAction<KotlinImportModelsBuildActionResult> {
    override fun execute(controller: BuildController): KotlinImportModelsBuildActionResult {
        fun request(modelId: String, parameters: ByteArray? = null): ByteArray = controller.getModel(
            KotlinGradleModel::class.java,
            ModelRequest::class.java,
        ) { request ->
            request.kotlinModelId = modelId
            request.kotlinModelParameters = parameters
        }.kotlinModelResult

        val base = request(KotlinImportModelIds.BASE)
        val projectInformation = request(KotlinImportModelIds.PROJECT_INFORMATION)
        val project = Result.parseFrom(projectInformation).model.unpack(ProjectModel::class.java)
        val compilationUnits = project.compilationUnitIdsList.map { compilationUnitId ->
            request(
                KotlinImportModelIds.COMPILATION_UNIT,
                CompilationUnitModelKt.parameters { this.compilationUnitId = compilationUnitId }.toByteArray(),
            )
        }
        val compilerArguments = project.compilationUnitIdsList.map { compilationUnitId ->
            request(
                KotlinImportModelIds.COMPILER_ARGUMENTS,
                CompilerArgumentsModelKt.parameters { this.compilationUnitId = compilationUnitId }.toByteArray(),
            )
        }
        fun dependencies(scope: DependenciesModel.Scope) = project.compilationUnitIdsList.map { compilationUnitId ->
            request(
                KotlinImportModelIds.DEPENDENCIES,
                DependenciesModelKt.parameters {
                    this.compilationUnitId = compilationUnitId
                    this.scope = scope
                    coverage = DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL
                }.toByteArray(),
            )
        }
        return KotlinImportModelsBuildActionResult(
            base,
            projectInformation,
            compilationUnits,
            compilerArguments,
            dependencies(DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE),
            dependencies(DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILER_PLUGIN),
        )
    }
}

private data class KotlinImportModelsBuildActionResult(
    val base: ByteArray,
    val project: ByteArray,
    val compilationUnits: List<ByteArray>,
    val compilerArguments: List<ByteArray>,
    val dependencies: List<ByteArray>,
    val compilerPluginDependencies: List<ByteArray>,
) : Serializable

private data class KotlinImportModelsModels(
    val base: Result,
    val project: Result,
    val compilationUnits: List<Result>,
    val compilerArguments: List<Result>,
    val dependencies: List<Result>,
    val compilerPluginDependencies: List<Result>,
)

private fun KotlinImportModelsBuildActionResult.toModels(): KotlinImportModelsModels = KotlinImportModelsModels(
    base = Result.parseFrom(base),
    project = Result.parseFrom(project),
    compilationUnits = compilationUnits.map(Result::parseFrom),
    compilerArguments = compilerArguments.map(Result::parseFrom),
    dependencies = dependencies.map(Result::parseFrom),
    compilerPluginDependencies = compilerPluginDependencies.map(Result::parseFrom),
)
