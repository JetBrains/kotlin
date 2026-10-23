/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.RunAll
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CustomBuilderMessage
import org.jetbrains.kotlin.config.SettingConstants
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.utils.TestLookupTracker
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.incremental.JpsIncrementalJvmCache
import org.jetbrains.kotlin.jps.incremental.KotlinDataContainerTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

class KotlinCompilerReferenceIndexJpsTest : AbstractKotlinJpsBuildTestCase() {
    private val enableICFixture = EnableICFixture()

    private val lookupTracker = TestLookupTracker()

    private val pathConverter by lazy { JpsFileToPathConverter(myProject) }
    private val icContext by lazy {
        IncrementalCompilationContext(pathConverterForSourceFiles = pathConverter, pathConverterForOutputFiles = pathConverter)
    }
    private val dataPaths by lazy { BuildDataPathsImpl(myDataStorageRoot.toPath()) }

    companion object {
        private const val BASE_SOURCE = """
            package a

            interface Base
            open class Impl : Base { fun member() {} }
            class Other : Base
            fun topLevel() {}
            val topProp: Int = 1
            object Obj { var prop: Int = 0 }
        """.trimIndent()

        private const val SAME_MODULE_SOURCE = """
            package a

            fun sameModule() = topProp
        """.trimIndent()

        private const val USAGE_SOURCE = """
            package b

            import a.Base
            import a.Impl
            import a.Obj
            import a.topLevel

            class Child : Impl()
            fun use(base: Base) {
                topLevel()
                Impl().member()
            }
            fun obj() = Obj.prop
        """.trimIndent()

        private const val UNTOUCHED_SOURCE = """
            package b

            import a.Base
            import a.topProp

            class Untouched : Base
            fun untouched() = topProp
        """.trimIndent()

        private const val USAGE_AFTER_INCREMENTAL_BUILD = """
            package b

            import a.Base
            import a.Obj
            import a.topProp

            class Child : Base
            fun use(base: Base) {}
            fun obj() = Obj.prop
            fun readTopProp() = topProp
        """.trimIndent()

        private val PROJECT_PACKAGES = setOf("a", "b")
        private val PROJECT_CLASSIFIERS = listOf("a.Base", "a.Impl", "a.Obj", "a.Other", "b.Child", "b.Untouched")
    }

    @BeforeEach
    override fun setUp(testInfo: TestInfo) {
        super.setUp(testInfo)
        workDir = KotlinTestUtils.tmpDirForTest(testInfo)
        enableICFixture.setUp()
        myProject.setTestingContext(TestingContext(lookupTracker, buildLogger = null))
    }

    @AfterEach
    override fun tearDown() {
        RunAll(
            { enableICFixture.tearDown() },
            { super.tearDown() }
        ).run()
    }

    @Test
    fun testIndexAfterInitialAndIncrementalBuild() {
        createProject()
        val initialBuild = buildAllModules()
        initialBuild.assertSuccessful()
        val afterInitialBuild = dumpIndex("after initial build", initialBuild)

        change(getAbsolutePath("b/src/Usage.kt"), USAGE_AFTER_INCREMENTAL_BUILD)
        val incrementalBuild = buildAllModules()
        incrementalBuild.assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "b/src/Usage.kt")
        val afterIncrementalBuild = dumpIndex("after incremental build", incrementalBuild)

        val expectedFile = File(TEST_DATA_PATH, "compilerReferenceIndex/indexAfterInitialAndIncrementalBuild.txt")
        TestDataAssertions.assertEqualsToFile(expectedFile, "$afterInitialBuild\n$afterIncrementalBuild")
    }

    private fun createProject() {
        createFile("a/src/Base.kt", BASE_SOURCE)
        createFile("a/src/SameModule.kt", SAME_MODULE_SOURCE)
        val moduleA = addModule("a", getAbsolutePath("a/src"))

        createFile("b/src/Usage.kt", USAGE_SOURCE)
        createFile("b/src/Untouched.kt", UNTOUCHED_SOURCE)
        val moduleB = addModule("b", getAbsolutePath("b/src"))
        JpsModuleRootModificationUtil.addDependency(moduleB, moduleA)

        addKotlinStdlibDependency()
    }

    private fun dumpIndex(title: String, buildResult: BuildResult): String = buildString {
        appendLine("=== $title ===")
        appendLine("lookups:")
        lookupLines().forEach { appendLine("  $it") }
        appendLine("subtypes:")
        val subtypes = subtypesByClassifier()
        for (classifier in PROJECT_CLASSIFIERS) {
            appendLine("  $classifier -> ${subtypes.getValue(classifier).sorted().joinToString()}")
        }
        appendLine("processed modules:")
        processedModules(buildResult).sorted().forEach { appendLine("  $it") }
    }

    private fun lookupLines(): List<String> {
        val targetDataRoot = dataPaths.getTargetDataRootDir(KotlinDataContainerTarget).toFile()
        val projectRoot = workDir.canonicalFile
        // The storage keeps only name hashes, so the symbols to query come from what the compiler reported to [lookupTracker].
        // Lookups in scopes outside the project, such as `kotlin`, are skipped on purpose
        val projectLookups = lookupTracker.lookups
            .filter { it.scopeFqName.substringBefore('.') in PROJECT_PACKAGES }
            .mapTo(mutableSetOf()) { LookupSymbol(it.name, it.scopeFqName) }
        return LookupStorage(targetDataRoot, icContext).use { lookupStorage ->
            projectLookups.mapNotNull { symbol ->
                val files = lookupStorage.get(symbol).map { path ->
                    pathConverter.toFile(path).canonicalFile.relativeTo(projectRoot).invariantSeparatorsPath
                }
                if (files.isEmpty()) null else "${symbol.scope}::${symbol.name} -> ${files.sorted().joinToString()}"
            }.sorted()
        }
    }

    private fun subtypesByClassifier(): Map<String, Set<String>> {
        val subtypes = PROJECT_CLASSIFIERS.associateWith { mutableSetOf<String>() }

        for (module in myProject.modules) {
            val target = ModuleBuildTarget(module, isTests = false)
            JpsIncrementalJvmCache(target, dataPaths, icContext).use { cache ->
                for (classifier in PROJECT_CLASSIFIERS) {
                    cache.getSubtypesOf(FqName(classifier)).mapTo(subtypes.getValue(classifier), FqName::asString)
                }
            }
        }
        return subtypes
    }

    // Every build in this test has all modules in scope, so all of them are reported regardless of what was compiled
    private fun processedModules(buildResult: BuildResult): Set<String> =
        buildResult.getMessages(BuildMessage.Kind.INFO)
            .filterIsInstance<CustomBuilderMessage>()
            .filter {
                it.builderId == SettingConstants.KOTLIN_COMPILER_REFERENCE_INDEX_BUILDER_ID &&
                        it.messageType == "processed module"
            }
            .mapTo(mutableSetOf()) { it.messageText }
}
