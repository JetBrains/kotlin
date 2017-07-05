/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

/**
 * This test checks that Java classes from sources have higher priority in Kotlin resolution process than classes from binaries.
 * To test this, we compile a Kotlin+Java module (in two modes: CLI and module-based) where a runtime Java class was replaced
 * with a "newer" version in sources, and check that this class resolves to the one from sources by calling a method absent in the runtime
 */
class ClasspathOrderTest : TestCaseWithTmpdir() {
    companion object {
        val sourceDir = File(KotlinTestUtils.getTestDataPathBase() + "/classpathOrder").absoluteFile
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
                listOf(PathUtil.kotlinPathsForDistDirectory.runtimePath),
                null,
                JavaModuleBuildTargetType.PRODUCTION.typeId,
                JavaModuleBuildTargetType.PRODUCTION.isTests,
                setOf(),
                emptyList()
        ).asText().toString()

        val xml = File(tmpdir, "module.xml")
        xml.writeText(xmlContent)

        MockLibraryUtil.compileKotlinModule(xml.absolutePath)
    }
}
