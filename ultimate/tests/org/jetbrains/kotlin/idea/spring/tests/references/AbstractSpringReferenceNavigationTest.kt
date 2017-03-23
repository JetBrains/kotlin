/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.tests.references

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.actions.AbstractNavigationTest
import org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractSpringReferenceNavigationTest : AbstractNavigationTest() {
    override fun getSourceAndTargetElements(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData? {
        val stringTemplate = file.findElementAt(editor.caretModel.offset)!!.getNonStrictParentOfType<KtStringTemplateExpression>()!!
        return GotoTargetHandler.GotoData(stringTemplate,
                                          stringTemplate.references.mapNotNull { it.resolve() }.toTypedArray(),
                                          emptyList())
    }

    override fun setUp() {
        super.setUp()
        TestFixtureExtension.loadFixture<SpringTestFixtureExtension>(myModule)
    }

    override fun configureExtra(mainFileBaseName: String, mainFileText: String) {
        if (!InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// NO_XML_CONFIG")) {
            TestFixtureExtension
                    .getFixture<SpringTestFixtureExtension>()!!
                    .configureFileSet(myFixture, listOf("${mainFileBaseName}_config.xml"))
        }
        if (InTextDirectivesUtils.isDirectiveDefined(mainFileText, "// JAVAX_ANNOTATION_RESOURCE")) {
            myFixture.configureByText(
                    KotlinFileType.INSTANCE,
                    """package javax.annotation; annotation class Resource(val name: String = "")"""
            )
        }
    }

    override fun tearDown() {
        TestFixtureExtension.unloadFixture<SpringTestFixtureExtension>()
        super.tearDown()
    }
}