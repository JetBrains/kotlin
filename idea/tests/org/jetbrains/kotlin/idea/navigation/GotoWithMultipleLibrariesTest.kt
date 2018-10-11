/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

    fun testOneHasSourceAndOneDoesnot() {
        doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(
                withSource = 1,
                noSource = 1
        )
    }

    fun testOneHasSourceAndManyDont() {
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

    fun doTestSameJarSharedByLibrariesWithAndWithoutSourceAttached(withSource: Int, noSource: Int) {
        val srcPath = testDataPath + "src"

        val sharedJar = MockLibraryUtil.compileJvmLibraryToJar(testDataPath + "libSrc", "sharedJar", addSources = true)
        val jarRoot = sharedJar.jarRoot

        var i: Int = 0
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

    protected fun module(name: String, srcPath: String) = createModuleFromTestData(srcPath, name, StdModuleTypes.JAVA, true)!!
}
