/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.compilerRunner.btapi.JpsBtaToolchainLoader
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize

@BuildToolsApiOnly
class BuildToolsApiCompilerReferenceIndexTest : AbstractKotlinJpsBuildTestCase() {
    private val dataPaths by lazy { BuildDataPathsImpl(myDataStorageRoot.toPath()) }

    @BeforeEach
    override fun setUp(testInfo: TestInfo) {
        super.setUp(testInfo)
        workDir = KotlinTestUtils.tmpDirForTest(testInfo)
    }

    @Test
    fun testIndexIsWrittenForEachModuleTarget() {
        val modules = createProject()
        withSystemProperty(JpsBtaToolchainLoader.GENERATE_COMPILER_REF_INDEX_PROPERTY, "true") {
            buildAllModules().assertSuccessful()
        }
        for (module in modules) {
            val lookups = criDirectory(module).resolve(CriToolchain.LOOKUPS_FILENAME)
            assertTrue(lookups.exists()) { "No compiler reference index for ${module.name} at $lookups" }
        }
    }

    @Test
    fun testNothingIsWrittenByDefault() {
        val modules = createProject()
        buildAllModules().assertSuccessful()
        for (module in modules) {
            assertFalse(criDirectory(module).exists()) { "Unexpected compiler reference index for ${module.name}" }
        }
    }

    @Test
    fun testRebuildStartsTheIndexOver() {
        val modules = createProject()
        withSystemProperty(JpsBtaToolchainLoader.GENERATE_COMPILER_REF_INDEX_PROPERTY, "true") {
            buildAllModules().assertSuccessful()
            val afterBuild = lookupsSizes(modules)
            // An incremental build appends instead of replacing, so the rebuild below has stale data to clear: KT-82000
            change(getAbsolutePath("b/src/Derived.kt"), "package b\n\nclass Derived : a.Base() { fun x() = 1 }\n")
            buildAllModules().assertSuccessful()
            rebuildAllModules()
            assertEquals(afterBuild, lookupsSizes(modules)) { "A rebuild must clear the data of the earlier builds" }
        }
    }

    private fun createProject(): List<JpsModule> {
        createFile("a/src/Base.kt", "package a\n\nopen class Base\n")
        val moduleA = addModule("a", getAbsolutePath("a/src"))
        createFile("b/src/Derived.kt", "package b\n\nclass Derived : a.Base()\n")
        val moduleB = addModule("b", getAbsolutePath("b/src"))
        JpsModuleRootModificationUtil.addDependency(moduleB, moduleA)
        addKotlinStdlibDependency()
        return listOf(moduleA, moduleB)
    }

    private fun lookupsSizes(modules: List<JpsModule>): List<Long> =
        modules.map { criDirectory(it).resolve(CriToolchain.LOOKUPS_FILENAME).fileSize() }

    private fun criDirectory(module: JpsModule): Path =
        dataPaths.getTargetDataRootDir(ModuleBuildTarget(module, false)).resolve(CriToolchain.DATA_PATH)
}
