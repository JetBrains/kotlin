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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.memberPullUp.PullUpConflictsUtil
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.RefactoringHierarchyUtil
import com.intellij.refactoring.util.classMembers.MemberInfoStorage
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.AbstractMemberPullPushTest
import org.jetbrains.kotlin.idea.refactoring.chooseMembers
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractPullUpTest : AbstractMemberPullPushTest() {
    private fun getTargetClassName(file: PsiFile) = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// TARGET_CLASS: ")

    protected fun doKotlinTest(path: String) {
        doTest(path) { file ->
            val targetClassName = getTargetClassName(file)
            val helper = object: KotlinPullUpHandler.TestHelper {
                override fun adjustMembers(members: List<KotlinMemberInfo>) = chooseMembers(members)

                override fun chooseSuperClass(superClasses: List<PsiNamedElement>): PsiNamedElement {
                    if (targetClassName != null) {
                        return superClasses.single { it.qualifiedClassNameForRendering() == targetClassName }
                    }
                    return superClasses.first()
                }
            }
            KotlinPullUpHandler().invoke(project, editor, file) {
                if (it == KotlinPullUpHandler.PULL_UP_TEST_HELPER_KEY) helper else null
            }
        }
    }

    // Based on com.intellij.refactoring.PullUpTest.doTest()
    protected fun doJavaTest(path: String) {
        doTest(path) { file ->
            val elementAt = getFile().findElementAt(editor.caretModel.offset)
            val sourceClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass::class.java)!!

            val targetClassName = getTargetClassName(file)
            val superClasses = RefactoringHierarchyUtil.createBasesList(sourceClass, false, true)
            val targetClass = targetClassName?.let { name -> superClasses.first { it.qualifiedName == name } } ?: superClasses.first()

            val storage = MemberInfoStorage(sourceClass) { true }
            val memberInfoList = chooseMembers(storage.getClassMemberInfos(sourceClass))
            val memberInfos = memberInfoList.toTypedArray()

            val targetDirectory = targetClass.containingFile.containingDirectory
            val conflicts = PullUpConflictsUtil.checkConflicts(
                    memberInfos,
                    sourceClass,
                    targetClass,
                    targetDirectory.getPackage()!!,
                    targetDirectory,
                    { psiMethod : PsiMethod -> PullUpProcessor.checkedInterfacesContain(memberInfoList, psiMethod) },
                    true
            )
            if (!conflicts.isEmpty) throw BaseRefactoringProcessor.ConflictsInTestsException(conflicts.values())

            PullUpProcessor(sourceClass, targetClass, memberInfos, DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)).run()
            UIUtil.dispatchAllInvocationEvents()
        }
    }
}
