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

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure
import com.intellij.ide.util.FileStructurePopup
import com.intellij.ide.actions.ViewStructureAction
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import com.intellij.openapi.ui.Queryable.PrintInfo
import org.jetbrains.kotlin.test.util.configureWithExtraFile

public abstract class AbstractKotlinFileStructureTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/structureView/fileStructure"

    public fun doTest(path: String) {
        myFixture.configureWithExtraFile(path)

        val textEditor = TextEditorProvider.getInstance()!!.getTextEditor(myFixture.getEditor())
        val popup = ViewStructureAction.createPopup(myFixture.getProject(), textEditor)

        if (popup == null) throw AssertionError("popup mustn't be null")

        popup.createCenterPanel()
        popup.getTreeBuilder().getUi()!!.getUpdater()!!.setPassThroughMode(true)
        popup.update()

        popup.setup()

        val printInfo = PrintInfo(arrayOf("text"), arrayOf("location"))
        val popupText = StructureViewUtil.print(popup.getTree(), false, printInfo, null)
        JetTestUtils.assertEqualsToFile(File("${FileUtil.getNameWithoutExtension(path)}.after"), popupText)
    }

    protected fun FileStructurePopup.setup() {
        val fileText = FileUtil.loadFile(File(getTestDataPath(), fileName()), true)

        val withInherited = InTextDirectivesUtils.isDirectiveDefined(fileText, "WITH_INHERITED")
        setTreeActionState(javaClass<KotlinInheritedMembersNodeProvider>(), withInherited)
    }

    public fun FileStructurePopup.update() {
        getTreeBuilder().refilter()!!.doWhenProcessed {
            getStructure().rebuild()
            updateRecursively(getRootNode())
            getTreeBuilder().updateFromRoot()

            TreeUtil.expandAll(getTree())
        }
    }

    fun FileStructurePopup.getFileStructureSpeedSearch() = getSpeedSearch() as FileStructurePopup.MyTreeSpeedSearch

    fun FileStructurePopup.getStructure() = getTreeBuilder().getTreeStructure() as FilteringTreeStructure

    fun FileStructurePopup.getRootNode() = getTreeBuilder().getRootElement() as FilteringTreeStructure.FilteringNode

    fun FileStructurePopup.updateRecursively(node: FilteringTreeStructure.FilteringNode) {
        node.update()
        for (child in node.children()!!) {
            updateRecursively(child)
        }
    }
}
