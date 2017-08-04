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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinMultiModuleChangeSignatureTest : KotlinMultiFileTestCase() {
    init {
        isMultiModule = true
    }

    override fun getTestRoot(): String = "/refactoring/changeSignatureMultiModule/"

    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase()

    private fun doTest(filePath: String, configure: KotlinChangeInfo.() -> Unit) {
        doTestCommittingDocuments { rootDir, _ ->
            val psiFile = rootDir.findFileByRelativePath(filePath)!!.toPsiFile(project)!!
            val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
            val marker = doc.extractMarkerOffset(project)
            assert(marker != -1)
            val element = KotlinChangeSignatureHandler().findTargetMember(psiFile.findElementAt(marker)!!) as KtElement
            val bindingContext = element.analyze(BodyResolveMode.FULL)
            val callableDescriptor = KotlinChangeSignatureHandler.findDescriptor(element, project, editor, bindingContext)!!
            val changeInfo = createChangeInfo(project, callableDescriptor, KotlinChangeSignatureConfiguration.Empty, element)!!
            KotlinChangeSignatureProcessor(project, changeInfo.apply { configure() }, "Change signature").run()
        }
    }

    fun testHeadersAndImplsByHeaderFun() = doTest("Common/src/test/test.kt") {
        newName = "baz"
    }

    fun testHeadersAndImplsByImplFun() = doTest("JS/src/test/test.kt") {
        newName = "baz"
    }

    fun testHeadersAndImplsByHeaderClassMemberFun() = doTest("Common/src/test/test.kt") {
        newName = "baz"
    }

    fun testHeadersAndImplsByImplClassMemberFun() = doTest("JS/src/test/test.kt") {
        newName = "baz"
    }
}
