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

package org.jetbrains.kotlin.idea.refactoring

import com.google.gson.JsonParser
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.findElementsByCommentPrefix
import java.io.File

abstract class AbstractMemberPullPushTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    val fixture: JavaCodeInsightTestFixture get() = myFixture

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase()

    protected fun doTest(path: String, action: (mainFile: PsiFile) -> Unit) {
        val mainFile = File(path)
        val afterFile = File("$path.after")
        val conflictFile = File("$path.messages")

        fixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${mainFile.parent}"

        val mainFileName = mainFile.name
        val mainFileBaseName = FileUtil.getNameWithoutExtension(mainFileName)
        val extraFiles = mainFile.parentFile.listFiles { file, name ->
            name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java"))
        }
        val extraFilesToPsi = extraFiles.associateBy { fixture.configureByFile(it.name) }
        val file = fixture.configureByFile(mainFileName)

        val addKotlinRuntime = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// WITH_RUNTIME") != null
        if (addKotlinRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        try {
            markMembersInfo(file)
            extraFilesToPsi.keys.forEach(::markMembersInfo)

            action(file)

            assert(!conflictFile.exists()) { "Conflict file $conflictFile should not exist" }
            KotlinTestUtils.assertEqualsToFile(afterFile, file.text!!)
            for ((extraPsiFile, extraFile) in extraFilesToPsi) {
                KotlinTestUtils.assertEqualsToFile(File("${extraFile.path}.after"), extraPsiFile.text)
            }
        }
        catch(e: Exception) {
            val message = when (e) {
                is BaseRefactoringProcessor.ConflictsInTestsException -> e.messages.sorted().joinToString("\n")
                is CommonRefactoringUtil.RefactoringErrorHintException -> e.message!!
                else -> throw e
            }
            KotlinTestUtils.assertEqualsToFile(conflictFile, message)
        }
        finally {
            if (addKotlinRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }


}

internal fun markMembersInfo(file: PsiFile) {
    for ((element, info) in file.findElementsByCommentPrefix("// INFO: ")) {
        val parsedInfo = JsonParser().parse(info).asJsonObject
        element.elementInfo = ElementInfo(parsedInfo["checked"]?.asBoolean ?: false,
                                          parsedInfo["toAbstract"]?.asBoolean ?: false)
    }
}

internal data class ElementInfo(val checked: Boolean, val toAbstract: Boolean)

internal var PsiElement.elementInfo: ElementInfo by NotNullableUserDataProperty(Key.create("ELEMENT_INFO"), ElementInfo(false, false))

internal fun <T : MemberInfoBase<*>> chooseMembers(members: List<T>): List<T> {
    members.forEach {
        val memberPsi = it.member.let { if (it is KtPsiClassWrapper) it.psiClass else it }
        val info = memberPsi.elementInfo
        it.isChecked = info.checked
        it.isToAbstract = info.toAbstract
    }
    return members.filter { it.isChecked }
}