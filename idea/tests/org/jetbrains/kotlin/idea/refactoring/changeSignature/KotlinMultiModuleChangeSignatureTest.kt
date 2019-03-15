/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

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

    private fun KotlinChangeInfo.addParameter(name: String, type: KotlinType, defaultValue: String) {
        addParameter(
                KotlinParameterInfo(
                        originalBaseFunctionDescriptor,
                        -1,
                        name,
                        KotlinTypeInfo(false, type),
                        defaultValueForCall = KtPsiFactory(project).createExpression(defaultValue)
                )
        )
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

    fun testHeaderPrimaryConstructorNoParams() = doTest("Common/src/test/test.kt") {
        addParameter("n", KotlinChangeSignatureTest.BUILT_INS.intType, "1")
    }

    fun testHeaderPrimaryConstructor() = doTest("Common/src/test/test.kt") {
        addParameter("b", KotlinChangeSignatureTest.BUILT_INS.booleanType, "false")
    }

    fun testHeaderSecondaryConstructor() = doTest("Common/src/test/test.kt") {
        addParameter("b", KotlinChangeSignatureTest.BUILT_INS.booleanType, "false")
    }

    fun testImplPrimaryConstructorNoParams() = doTest("JVM/src/test/test.kt") {
        addParameter("n", KotlinChangeSignatureTest.BUILT_INS.intType, "1")
    }

    fun testImplPrimaryConstructor() = doTest("JVM/src/test/test.kt") {
        addParameter("b", KotlinChangeSignatureTest.BUILT_INS.booleanType, "false")
    }

    fun testImplSecondaryConstructor() = doTest("JS/src/test/test.kt") {
        addParameter("b", KotlinChangeSignatureTest.BUILT_INS.booleanType, "false")
    }

    fun testSuspendImpls() = doTest("Common/src/test/test.kt") {
        addParameter("n", KotlinChangeSignatureTest.BUILT_INS.intType, "0")
    }
}
