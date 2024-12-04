/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.jps.build.AbstractKotlinJpsBuildTestCase
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

/**
 * This test checks that Java classes from sources have higher priority in Kotlin resolution process than classes from binaries.
 * To test this, we compile a Kotlin+Java module (in two modes: CLI and module-based) where a runtime Java class was replaced
 * with a "newer" version in sources, and check that this class resolves to the one from sources by calling a method absent in the runtime
 */
class ClasspathOrderTest : TestCaseWithTmpdir() {
    companion object {
        private val sourceDir
            get() = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "/../../../compiler/testData/classpathOrder").absoluteFile
    }

    fun testClasspathOrderForCLI() {
        MockLibraryUtil.compileKotlin(sourceDir.path, tmpdir)
    }

    fun testClasspathOrderForModuleScriptBuild() {
        val xmlContent = KotlinModuleXmlBuilder().addModule(
            "name",
            File(tmpdir, "output").absolutePath,
            listOf(sourceDir),
            listOf(JvmSourceRoot(sourceDir)),
            listOf(PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath),
            emptyList(),
            null,
            JavaModuleBuildTargetType.PRODUCTION.typeId,
            JavaModuleBuildTargetType.PRODUCTION.isTests,
            setOf(),
            emptyList(),
            IncrementalCompilation.isEnabledForJvm()
        ).asText().toString()

        val xml = File(tmpdir, "module.xml")
        xml.writeText(xmlContent)

        MockLibraryUtil.compileKotlinModule(xml.absolutePath)
    }
}
