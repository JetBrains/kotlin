/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.sources.kpm

import org.jetbrains.kotlin.gradle.MultiplatformExtensionTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.gradle.kpm.MultiplatformSourceSetMappedFragmentLocator
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule
import org.jetbrains.kotlin.gradle.kpm.kpmModules
import org.jetbrains.kotlin.gradle.kpm.SourceSetMappedFragmentLocator

internal open class MultiplatformSourceSetMappedFragmentLocatorTest : MultiplatformExtensionTest() {

    protected val locator = MultiplatformSourceSetMappedFragmentLocator()

    @BeforeTest
    override fun setup() {
        project.extensions.extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_KPM_EXPERIMENTAL_MODEL_MAPPING, "true")
        super.setup()
    }

    protected fun doTest(sourceSetName: String, checkResult: SourceSetMappedFragmentLocator.FragmentLocation.() -> Unit) {
        val result = checkNotNull(locator.locateFragmentForSourceSet(project, sourceSetName))
        checkResult(result)
    }

    protected fun doTest(sourceSetName: String, fragmentName: String, moduleName: String) {
        doTest(sourceSetName) {
            assertEquals(fragmentName, this.fragmentName)
            assertEquals(moduleName, this.moduleName)
        }
    }

    @Test
    fun `name with one part is returned as fragment name with main module`() {
        doTest("foo", "foo", KotlinGradleModule.MAIN_MODULE_NAME)
    }

    @Test
    fun `main is truncated from name`() {
        doTest("fooBarMain", "fooBar", KotlinGradleModule.MAIN_MODULE_NAME)
    }

    @Test
    fun `module chosen for test fragment`() {
        doTest("abcDefGhiTest", "abcDefGhi", KotlinGradleModule.TEST_MODULE_NAME)
    }

    @Test
    fun `digits accepted`() {
        val nameWithDigits = "testDigits123Accepted123"
        project.kpmModules.create(nameWithDigits)
        doTest("foo${nameWithDigits.capitalize()}", "foo", nameWithDigits)
        doTest("${nameWithDigits}Main", nameWithDigits, KotlinGradleModule.MAIN_MODULE_NAME)
    }

    @Test
    fun `capitalized name handling`() {
        doTest("IAmTheMain", "IAmThe", KotlinGradleModule.MAIN_MODULE_NAME)
    }

    @Test
    fun `live module logic update`() {
        val nameForTest = "someNewTest"
        doTest(nameForTest, "someNew", "test")
        project.kpmModules.create("newTest")
        doTest(nameForTest, "some", "newTest")

        // also check for the same logic in fragments that go to main if no suffix matched:

        val nameForMain = "fooBarBaz"
        doTest(nameForMain, nameForMain, KotlinGradleModule.MAIN_MODULE_NAME)
        val moduleSuffixName = "barBaz"
        project.kpmModules.create(moduleSuffixName)
        doTest(nameForMain, "foo", moduleSuffixName)
    }
}

internal class MultiplatformSourceSetMappedFragmentLocatorTestWithAndroid : MultiplatformSourceSetMappedFragmentLocatorTest() {

    override fun setup() {
        super.setup()
        project.plugins.apply("android-library")
        kotlin.android()
    }

    @Test
    fun `android source set names`() {
        doTest("androidMain", "android", KotlinGradleModule.MAIN_MODULE_NAME)
        doTest("androidTest", "android", KotlinGradleModule.TEST_MODULE_NAME)
        doTest("androidAndroidTest", "androidAndroid", KotlinGradleModule.TEST_MODULE_NAME)

        doTest("androidDebug", "androidDebug", KotlinGradleModule.MAIN_MODULE_NAME)
        doTest("androidDebugTest", "androidDebug", KotlinGradleModule.TEST_MODULE_NAME)
        doTest("androidDebugAndroidTest", "androidDebugAndroid", KotlinGradleModule.TEST_MODULE_NAME)
    }
}