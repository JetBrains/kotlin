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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.FileStructureTestFixture
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

abstract class KotlinFileStructureTestBase : KotlinLightCodeInsightFixtureTestCase() {

    protected var myPopupFixture: FileStructureTestFixture? = null
    protected val popupFixture get() = myPopupFixture!!

    protected abstract val fileExtension: String

    open protected val treeFileName: String
        get() = getFileName("tree")

    override fun setUp() {
        super.setUp()
        myPopupFixture = FileStructureTestFixture(myFixture)
    }

    protected fun configureDefault() {
        myFixture.configureByFile(getFileName(fileExtension))
    }

    public override fun tearDown() {
        try {
            Disposer.dispose(popupFixture)
            myPopupFixture = null
        }
        finally {
            super.tearDown()
        }
    }

    protected fun getFileName(ext: String): String {
        return getTestName(false) + if (StringUtil.isEmpty(ext)) "" else "." + ext
    }

    @Suppress("unused")
    protected fun checkTree(filter: String) {
        configureDefault()
        popupFixture.update()
        popupFixture.popup.setSearchFilterForTests(filter)
        PlatformTestUtil.waitForPromise(popupFixture.popup.rebuildAndUpdate())
        popupFixture.speedSearch.findAndSelectElement(filter)
        checkResult()
    }

    protected fun checkTree() {
        configureDefault()
        popupFixture.update()
        checkResult()
    }

    protected fun checkResult() {
        val printInfo = Queryable.PrintInfo(arrayOf("text"), arrayOf("location"))
        val popupText = StructureViewUtil.print(popupFixture.tree, false, printInfo, null).trim { it <= ' ' }
        UsefulTestCase.assertSameLinesWithFile(testDataPath + "/" + treeFileName, popupText)
    }
}