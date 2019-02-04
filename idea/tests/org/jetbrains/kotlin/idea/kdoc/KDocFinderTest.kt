/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
