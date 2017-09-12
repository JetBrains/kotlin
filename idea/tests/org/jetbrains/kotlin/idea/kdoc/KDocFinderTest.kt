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

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Assert

class KDocFinderTest() : LightPlatformCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/finder/"
    }

    fun testConstructor() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val declaration = (myFixture.file as KtFile).declarations[0]
        val descriptor = declaration.unsafeResolveToDescriptor() as ClassDescriptor
        val constructorDescriptor = descriptor.unsubstitutedPrimaryConstructor!!
        val doc = constructorDescriptor.findKDoc()
        Assert.assertEquals("Doc for constructor of class C.", doc!!.getContent())
    }

    fun testAnnotated() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val declaration = (myFixture.file as KtFile).declarations[0]
        val descriptor = declaration.unsafeResolveToDescriptor() as ClassDescriptor
        val overriddenFunctionDescriptor = descriptor.defaultType.memberScope.getContributedFunctions(Name.identifier("xyzzy"), NoLookupLocation.FROM_TEST).single()
        val doc = overriddenFunctionDescriptor.findKDoc()
        Assert.assertEquals("Doc for method xyzzy", doc!!.getContent())
    }

    fun testOverridden() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val declaration = (myFixture.file as KtFile).declarations.single { it.name == "Bar" }
        val descriptor = declaration.unsafeResolveToDescriptor() as ClassDescriptor
        val overriddenFunctionDescriptor = descriptor.defaultType.memberScope.getContributedFunctions(Name.identifier("xyzzy"), NoLookupLocation.FROM_TEST).single()
        val doc = overriddenFunctionDescriptor.findKDoc()
        Assert.assertEquals("Doc for method xyzzy", doc!!.getContent())
    }

    fun testOverriddenWithSubstitutedType() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val declaration = (myFixture.file as KtFile).declarations.single { it.name == "Bar" }
        val descriptor = declaration.unsafeResolveToDescriptor() as ClassDescriptor
        val overriddenFunctionDescriptor = descriptor.defaultType.memberScope.getContributedFunctions(Name.identifier("xyzzy"), NoLookupLocation.FROM_TEST).single()
        val doc = overriddenFunctionDescriptor.findKDoc()
        Assert.assertEquals("Doc for method xyzzy", doc!!.getContent())
    }

    fun testProperty() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val declaration = (myFixture.file as KtFile).declarations.single { it.name == "Foo" }
        val descriptor = declaration.unsafeResolveToDescriptor() as ClassDescriptor
        val propertyDescriptor = descriptor.defaultType.memberScope.getContributedVariables(Name.identifier("xyzzy"), NoLookupLocation.FROM_TEST).single()
        val doc = propertyDescriptor.findKDoc()
        Assert.assertEquals("Doc for property xyzzy", doc!!.getContent())
    }
}
