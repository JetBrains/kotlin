/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KotlinImportModelProviderTest {
    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces Kotlin Multiplatform models`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
                sourceSets.getByName("commonMain").generatedKotlin.srcDir(
                    tasks.register("generateCommonMainSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/commonMain/generated-kotlin"))
                    }
                )
                sourceSets.getByName("jvmMain").generatedKotlin.srcDir(
                    tasks.register("generateJvmMainSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/jvmMain/generated-kotlin"))
                    }
                )
                sourceSets.getByName("jvmMain").generatedKotlin.srcDir(
                    tasks.register("generateAdditionalJvmMainSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/jvmMain/additional-generated-kotlin"))
                    }
                )
                sourceSets.getByName("linuxX64Main").generatedKotlin.srcDir(
                    tasks.register("generateLinuxX64MainSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/linuxX64Main/generated-kotlin"))
                    }
                )
                sourceSets.getByName("commonTest").generatedKotlin.srcDir(
                    tasks.register("generateCommonTestSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/commonTest/generated-kotlin"))
                    }
                )
                sourceSets.getByName("jvmTest").generatedKotlin.srcDir(
                    tasks.register("generateJvmTestSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/jvmTest/generated-kotlin"))
                    }
                )
                sourceSets.getByName("linuxX64Test").generatedKotlin.srcDir(
                    tasks.register("generateLinuxX64TestSources") {
                        it.outputs.dir(layout.projectDirectory.dir("src/linuxX64Test/generated-kotlin"))
                    }
                )
            }
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)
        val kotlin = project.multiplatformExtension

        val commonMainId = compilationUnitId { value = ":|:|metadata|commonMain" }
        val jvmMainId = compilationUnitId { value = ":|:|jvm|main" }
        val jvmTestId = compilationUnitId { value = ":|:|jvm|test" }
        val linuxX64MainId = compilationUnitId { value = ":|:|linuxX64|main" }
        val linuxX64TestId = compilationUnitId { value = ":|:|linuxX64|test" }
        assertEquals(
            listOf(BaseModel.Capability.CAPABILITY_KOTLIN_MULTIPLATFORM),
            provider.baseInformation().capabilitiesList,
        )
        assertEquals(
            listOf(jvmMainId, jvmTestId, linuxX64MainId, linuxX64TestId, commonMainId),
            provider.projectInformation().compilationUnitIdsList,
        )

        val commonMainTask = kotlin.metadata().compilations.getByName("commonMain").compileTaskProvider.get() as KotlinCompileCommon
        val jvmMainTask = kotlin.jvm().compilations.getByName("main").compileTaskProvider.get() as KotlinJvmCompile
        val jvmTestTask = kotlin.jvm().compilations.getByName("test").compileTaskProvider.get() as KotlinJvmCompile
        val linuxX64MainTask = kotlin.linuxX64().compilations.getByName("main").compileTaskProvider.get()
        val linuxX64TestTask = kotlin.linuxX64().compilations.getByName("test").compileTaskProvider.get()

        assertKmpCompilationUnit(
            provider.compilationUnit(commonMainId),
            commonMainId,
            "commonMain",
            CompilationUnitModel.Platform.PLATFORM_METADATA,
            listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM, CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_NATIVE),
            null,
            output(
                project.relativePath(commonMainTask.destinationDirectory.get().asFile),
                CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB,
                commonMainTask.path,
            ),
            listOf(
                sourceRoot(
                    "src/commonMain/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateCommonMainSources")
                ),
                sourceRoot("src/commonMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
            ),
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
        )
        assertKmpCompilationUnit(
            provider.compilationUnit(jvmMainId),
            jvmMainId,
            "main",
            CompilationUnitModel.Platform.PLATFORM_JVM,
            listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
            "jvm",
            output(
                project.relativePath(jvmMainTask.destinationDirectory.get().asFile),
                CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES,
                jvmMainTask.path,
            ),
            listOf(
                sourceRoot(
                    "src/commonMain/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateCommonMainSources")
                ),
                sourceRoot("src/commonMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot(
                    "src/jvmMain/additional-generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateAdditionalJvmMainSources"),
                    gradleAction(":generateJvmMainSources"),
                ),
                sourceRoot(
                    "src/jvmMain/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateAdditionalJvmMainSources"),
                    gradleAction(":generateJvmMainSources"),
                ),
                sourceRoot("src/jvmMain/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/jvmMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
            ),
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
        )
        assertKmpCompilationUnit(
            provider.compilationUnit(jvmTestId),
            jvmTestId,
            "test",
            CompilationUnitModel.Platform.PLATFORM_JVM,
            listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
            "jvm",
            output(
                project.relativePath(jvmTestTask.destinationDirectory.get().asFile),
                CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES,
                jvmTestTask.path,
            ),
            listOf(
                sourceRoot(
                    "src/commonTest/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateCommonTestSources")
                ),
                sourceRoot("src/commonTest/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot(
                    "src/jvmTest/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateJvmTestSources")
                ),
                sourceRoot("src/jvmTest/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/jvmTest/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
            ),
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST,
        )
        assertKmpCompilationUnit(
            provider.compilationUnit(linuxX64MainId),
            linuxX64MainId,
            "main",
            CompilationUnitModel.Platform.PLATFORM_NATIVE,
            listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_NATIVE),
            "linuxX64",
            output(
                project.relativePath(linuxX64MainTask.outputFile.get()),
                CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB,
                linuxX64MainTask.path,
            ),
            listOf(
                sourceRoot(
                    "src/commonMain/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateCommonMainSources")
                ),
                sourceRoot("src/commonMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/linuxMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot(
                    "src/linuxX64Main/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateLinuxX64MainSources")
                ),
                sourceRoot("src/linuxX64Main/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/nativeMain/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
            ),
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
        )
        assertKmpCompilationUnit(
            provider.compilationUnit(linuxX64TestId),
            linuxX64TestId,
            "test",
            CompilationUnitModel.Platform.PLATFORM_NATIVE,
            listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_NATIVE),
            "linuxX64",
            output(
                project.relativePath(linuxX64TestTask.outputFile.get()),
                CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB,
                linuxX64TestTask.path,
            ),
            listOf(
                sourceRoot(
                    "src/commonTest/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateCommonTestSources")
                ),
                sourceRoot("src/commonTest/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/linuxTest/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot(
                    "src/linuxX64Test/generated-kotlin",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    gradleAction(":generateLinuxX64TestSources")
                ),
                sourceRoot("src/linuxX64Test/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                sourceRoot("src/nativeTest/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
            ),
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST,
        )

        listOf(commonMainId, jvmMainId, linuxX64MainId).forEach { compilationUnitId ->
            assertEquals(
                emptyList(),
                provider.dependencies(DependenciesModelKt.parameters { this.compilationUnitId = compilationUnitId }).sourceDependenciesList,
            )
        }
        assertEquals(
            listOf(sourceDependency(jvmMainId)),
            provider.dependencies(DependenciesModelKt.parameters { this.compilationUnitId = jvmTestId }).sourceDependenciesList,
        )
        assertEquals(
            listOf(sourceDependency(linuxX64MainId)),
            provider.dependencies(DependenciesModelKt.parameters { this.compilationUnitId = linuxX64TestId }).sourceDependenciesList,
        )
    }

    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces stable main and test JVM models`() {
        val project = buildProjectWithJvm {
            extraProperties.set("kotlin.compiler.runViaBuildToolsApi", true)
            extraProperties.set("kotlin.compiler.generateCompilerRefIndex", true)
            kotlinJvmExtension.target.compilations.create("deploy")
            val generateMainSources = tasks.register("generateMainSources") {
                it.outputs.dir(layout.projectDirectory.dir("src/main/generated-kotlin"))
            }
            kotlinJvmExtension.sourceSets.getByName("main").generatedKotlin.srcDir(generateMainSources)
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

        val base = provider.baseInformation()
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(project.kotlinToolingVersion.major, base.pluginVersion.major)
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), base.capabilitiesList)

        val projectModel = provider.projectInformation()
        val mainId = compilationUnitId { value = ":|:|jvm|main" }
        val testId = compilationUnitId { value = ":|:|jvm|test" }
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectModel.id)
        assertEquals(listOf(mainId, testId), projectModel.compilationUnitIdsList)

        assertCompilationUnit(
            provider.compilationUnit(mainId),
            mainId,
            "main",
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
            listOf(
                output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin"),
                output("build/kotlin/compileKotlin/cacheable/cri", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI, ":compileKotlin"),
            ),
        )
        assertCompilationUnit(
            provider.compilationUnit(testId),
            testId,
            "test",
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST,
            listOf(
                output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin"),
                output(
                    "build/kotlin/compileTestKotlin/cacheable/cri",
                    CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI,
                    ":compileTestKotlin"
                ),
            ),
        )
        listOf(
            "classes/kotlin/main",
            "kotlin/compileKotlin/cacheable/cri",
            "classes/kotlin/test",
            "kotlin/compileTestKotlin/cacheable/cri",
        ).forEach { relativePath ->
            assertFalse(project.layout.buildDirectory.dir(relativePath).get().asFile.exists())
        }
    }

    @Test
    fun `does not declare CRI outputs when Build Tools API is disabled`() {
        val project = buildProjectWithJvm {
            extraProperties.set("kotlin.compiler.runViaBuildToolsApi", false)
            extraProperties.set("kotlin.compiler.generateCompilerRefIndex", true)
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

        val compilationUnits = provider.projectInformation().compilationUnitIdsList.associateBy { compilationUnitId ->
            provider.compilationUnit(compilationUnitId).name
        }

        assertEquals(
            listOf(output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin")),
            provider.compilationUnit(compilationUnits.getValue("main")).outputsList,
        )
        assertEquals(
            listOf(output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin")),
            provider.compilationUnit(compilationUnits.getValue("test")).outputsList,
        )
    }

    @Test
    fun `produces effective compiler arguments for a JVM compilation`() {
        val project = buildProjectWithJvm {
            kotlinJvmExtension.compilerOptions {
                optIn.add("my.custom.OptInAnnotation")
                freeCompilerArgs.add("-Xdebug")
            }
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)
        val mainId = provider.projectInformation().compilationUnitIdsList.first()

        val model = provider.compilerArguments(mainId)

        assertEquals(KotlinImportModelIds.COMPILER_ARGUMENTS, model.id)
        assertEquals(mainId, model.parameters.compilationUnitId)
        assertTrue("-Xdebug" in model.argumentsList)
        assertTrue("-opt-in my.custom.OptInAnnotation" in model.argumentsList.joinToString(" "))
    }

    @Test
    fun `escapes compilation unit ID components without collisions`() {
        assertEquals(
            ":included%7Cbuild|:app%25demo|jvm|test%7Cfixture",
            compilationUnitIdValue(":included|build", ":app%demo", "jvm", "test|fixture"),
        )
        assertNotEquals(
            compilationUnitIdValue("a|b", "c", "d", "e"),
            compilationUnitIdValue("a", "b", "c", "d|e"),
        )
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedPurpose: CompilationUnitModel.Purpose,
        expectedOutputs: List<CompilationUnitModel.Output>,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.name)
        assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, model.platform)
        assertEquals(listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM), model.targetPlatformsList)
        assertTrue(model.hasTargetName())
        assertEquals("jvm", model.targetName)
        assertEquals(expectedPurpose, model.purpose)
        assertEquals(expectedOutputs, model.outputsList)
        assertEquals(
            when (expectedName) {
                "main" -> listOf(
                    sourceRoot(
                        "src/main/generated-kotlin",
                        SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                        gradleAction(":generateMainSources"),
                    ),
                    sourceRoot("src/main/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/main/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                )
                "test" -> listOf(
                    sourceRoot("src/test/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/test/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                )
                else -> error("Unexpected compilation name: $expectedName")
            },
            model.sourceRootsList,
        )
    }

    private fun assertKmpCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedPlatform: CompilationUnitModel.Platform,
        expectedTargetPlatforms: List<CompilationUnitModel.TargetPlatform>,
        expectedTargetName: String?,
        expectedOutput: CompilationUnitModel.Output,
        expectedSourceRoots: List<SourceRoot>,
        expectedPurpose: CompilationUnitModel.Purpose,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.name)
        assertEquals(expectedPlatform, model.platform)
        assertEquals(expectedTargetPlatforms, model.targetPlatformsList)
        assertEquals(expectedTargetName != null, model.hasTargetName())
        if (expectedTargetName != null) assertEquals(expectedTargetName, model.targetName)
        assertEquals(expectedPurpose, model.purpose)
        assertEquals(listOf(expectedOutput), model.outputsList)
        assertEquals(expectedSourceRoots, model.sourceRootsList)
    }

    private fun sourceRoot(path: String, kind: SourceRoot.Kind, vararg producingActions: Action): SourceRoot = sourceRootModel {
        this.path = path
        this.kind = kind
        this.producingActions += producingActions.asIterable()
    }

    private fun gradleAction(taskPath: String): Action = action {
        gradleAction = ActionKt.gradleTask { this.taskPath = taskPath }
    }

    private fun sourceDependency(targetCompilationUnitId: CompilationUnitId): DependenciesModel.SourceDependency =
        DependenciesModelKt.sourceDependency {
            kind = DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_FRIEND
            this.targetCompilationUnitId = targetCompilationUnitId
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
}
