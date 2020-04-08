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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class KDocElementFactory(val project: Project) {
    fun createKDocFromText(text: String): KDoc {
        val fileText = "$text fun foo { }"
        val function = KtPsiFactory(project).createDeclaration<KtFunction>(fileText)
        return PsiTreeUtil.findChildOfType(function, KDoc::class.java)!!
    }

    fun createNameFromText(text: String): KDocName {
        val kdocText = "/** @param $text foo*/"
        val kdoc = createKDocFromText(kdocText)
        val section = kdoc.getDefaultSection()
        val tag = section.findTagByName("param")
        val link = tag?.getSubjectLink() ?: throw IllegalArgumentException("Cannot find subject link in doc comment '$kdocText'")
        return link.getChildOfType()!!
    }
}
