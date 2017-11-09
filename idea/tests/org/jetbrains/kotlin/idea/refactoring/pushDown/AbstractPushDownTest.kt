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

package org.jetbrains.kotlin.idea.refactoring.pushDown

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.memberPushDown.PushDownProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.classMembers.MemberInfoStorage
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.refactoring.AbstractMemberPullPushTest
import org.jetbrains.kotlin.idea.refactoring.chooseMembers
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo

abstract class AbstractPushDownTest : AbstractMemberPullPushTest() {
    protected fun doKotlinTest(path: String) {
        doTest(path) { file ->
            val helper = object: KotlinPushDownHandler.TestHelper {
                override fun adjustMembers(members: List<KotlinMemberInfo>) = chooseMembers(members)
            }
            KotlinPushDownHandler().invoke(project, editor, file) {
                if (it == KotlinPushDownHandler.PUSH_DOWN_TEST_HELPER_KEY) helper else null
            }
        }
    }

    protected fun doJavaTest(path: String) {
        doTest(path) {
            val elementAt = file.findElementAt(editor.caretModel.offset)
            val sourceClass = PsiTreeUtil.getParentOfType(elementAt, PsiClass::class.java)!!
            val storage = MemberInfoStorage(sourceClass) { true }
            val memberInfos = chooseMembers(storage.getClassMemberInfos(sourceClass))

            PushDownProcessor(sourceClass, memberInfos, DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)).run()
            UIUtil.dispatchAllInvocationEvents()
        }
    }
}