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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import kotlin.test.assertTrue

class NavigateFromLibrarySourcesTest: AbstractNavigateFromLibrarySourcesTest() {
    fun testJdkClass() {
        checkNavigationFromLibrarySource("Thread", "java.lang.Thread")
    }

    fun testOurKotlinClass() {
        checkNavigationFromLibrarySource("Foo", "a.Foo")
    }

    fun testBuiltinClass() {
        checkNavigationFromLibrarySource("String", "kotlin.String")
    }

    // This test is not exactly for navigation, but separating it to another class doesn't worth it.
    fun testLightClassForLibrarySource() {
        val navigationElement = navigationElementForReferenceInLibrarySource("usage.kt", "Foo")
        assertTrue(navigationElement is KtClassOrObject, "Foo should navigate to JetClassOrObject")
        val lightClass = (navigationElement as KtClassOrObject).toLightClass()
        assertTrue(lightClass is KtLightClassForDecompiledDeclaration,
                   "Light classes for decompiled declaration should be provided for library source")
        assertEquals("Foo", lightClass!!.name)
    }

    private fun checkNavigationFromLibrarySource(referenceText: String, targetFqName: String) {
        checkNavigationElement(navigationElementForReferenceInLibrarySource("usage.kt", referenceText), targetFqName)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/fromLibSource",
            true,
            true,
            false,
            false
        )
    }

    private fun navigationElementForReferenceInLibrarySource(referenceText: String) =
        navigationElementForReferenceInLibrarySource("usage.kt", referenceText)

    private fun checkNavigationElement(element: PsiElement, expectedFqName: String) {
        when (element) {
            is PsiClass -> {
                assertEquals(expectedFqName, element.qualifiedName)
            }
            is KtClass -> {
                assertEquals(expectedFqName, element.fqName!!.asString())
            }
            else -> {
                fail("Navigation element should be JetClass or PsiClass: " + element::class.java + ", " + element.text)
            }
        }
    }
}
