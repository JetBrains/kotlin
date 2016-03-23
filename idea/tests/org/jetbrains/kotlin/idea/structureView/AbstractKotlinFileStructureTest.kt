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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.actions.ViewStructureAction
import com.intellij.ide.util.FileStructurePopup
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.ui.Queryable.PrintInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractKotlinFileStructureTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/structureView/fileStructure"

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(path: String) {
        myFixture.configureWithExtraFile(path)

        val textEditor = TextEditorProvider.getInstance()!!.getTextEditor(myFixture.editor)
        val popup = ViewStructureAction.createPopup(myFixture.project, textEditor)

        if (popup == null) throw AssertionError("popup mustn't be null")

        popup.createCenterPanel()
        popup.treeBuilder.ui!!.updater!!.setPassThroughMode(true)
        popup.update()

        popup.setup()

        val printInfo = PrintInfo(arrayOf("text"), arrayOf("location"))
        val popupText = StructureViewUtil.print(popup.tree, false, printInfo, null)
        KotlinTestUtils.assertEqualsToFile(File("${FileUtil.getNameWithoutExtension(path)}.after"), popupText)
    }

    protected fun FileStructurePopup.setup() {
        val fileText = FileUtil.loadFile(File(testDataPath, fileName()), true)

        val withInherited = InTextDirectivesUtils.isDirectiveDefined(fileText, "WITH_INHERITED")
        setTreeActionState(KotlinInheritedMembersNodeProvider::class.java, withInherited)
    }

    fun FileStructurePopup.update() {
        treeBuilder.refilter()!!.doWhenProcessed {
            getStructure().rebuild()
            updateRecursively(getRootNode())
            treeBuilder.updateFromRoot()

            TreeUtil.expandAll(tree)
        }
    }

    fun FileStructurePopup.getFileStructureSpeedSearch() = speedSearch as FileStructurePopup.MyTreeSpeedSearch

    fun FileStructurePopup.getStructure() = treeBuilder.treeStructure as FilteringTreeStructure

    fun FileStructurePopup.getRootNode() = treeBuilder.rootElement as FilteringTreeStructure.FilteringNode

    fun FileStructurePopup.updateRecursively(node: FilteringTreeStructure.FilteringNode) {
        node.update()
        for (child in node.children()!!) {
            updateRecursively(child)
        }
    }
}
