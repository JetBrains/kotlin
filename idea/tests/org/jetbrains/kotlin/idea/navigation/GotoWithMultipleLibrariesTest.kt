/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.ide.util.gotoByName.GotoSymbolModel2
import com.intellij.openapi.module.StdModuleTypes
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.jarRoot
import org.jetbrains.kotlin.test.util.projectLibrary

class GotoWithMultipleLibrariesTest : AbstractMultiModuleTest() {
    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleReferenceResolve/sameJarInDifferentLibraries/"

    fun testOneHasSourceAndOneDoesNot() {
        doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(
            withSource = 1,
            noSource = 1
        )
    }

    fun testOneHasSourceAndManyDoNot() {
        doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(
            withSource = 1,
            noSource = 3
        )
    }

    fun testSeveralWithSource() {
        doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(
            withSource = 2,
            noSource = 2
        )
    }

    private fun doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(withSource: Int, noSource: Int) {
        val srcPath = testDataPath + "src"

        val sharedJar = MockLibraryUtil.compileJvmLibraryToJar(testDataPath + "libSrc", "sharedJar", addSources = true)
        val jarRoot = sharedJar.jarRoot

        var i = 0
        repeat(noSource) {
            module("m${++i}", srcPath).addDependency(projectLibrary("libA", jarRoot))
        }
        repeat(withSource) {
            module("m${++i}", srcPath).addDependency(projectLibrary("libB", jarRoot, jarRoot.findChild("src")!!))
        }

        checkFiles({ project.allKotlinFiles() }) {
            GotoCheck.checkGotoDirectives(GotoSymbolModel2(project), editor, nonProjectSymbols = true, checkNavigation = true)
        }
    }

    private fun module(name: String, srcPath: String) = createModuleFromTestData(srcPath, name, StdModuleTypes.JAVA, true)!!
}
