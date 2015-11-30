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

package org.jetbrains.kotlin.android

import com.intellij.codeInsight.TargetElementUtil
import org.jetbrains.kotlin.psi.KtProperty
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.xml.XmlAttributeValue
import org.apache.xmlbeans.impl.common.ResolverUtil
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.KotlinTestUtils

public abstract class AbstractAndroidGotoTest : KotlinAndroidTestCase() {

    override fun getTestDataPath(): String {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/goto/" + getTestName(true) + "/"
    }

    public fun doTest(path: String) {
        val f = myFixture!!
        getResourceDirs(path).forEach { myFixture.copyDirectoryToProject(it.name, it.name) }
        val virtualFile = f.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt");
        f.configureFromExistingVirtualFile(virtualFile)

        val expression = TargetElementUtil.findReference(f.editor, f.caretOffset)!!.element as KtElement
        val bindingContext = expression.analyzeFully()
        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, bindingContext[BindingContext.CALL, expression]]!!
        val property = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: throw AssertionError("PropertyDescriptor expected")

        val targetElement = GotoDeclarationAction.findTargetElement(f.project, f.editor, f.caretOffset)!!

        assert(targetElement is XmlAttributeValue) { "XmlAttributeValue expected, got ${targetElement.javaClass}" }
        assertEquals("@+id/${property.name}", (targetElement as XmlAttributeValue).value)
    }
}