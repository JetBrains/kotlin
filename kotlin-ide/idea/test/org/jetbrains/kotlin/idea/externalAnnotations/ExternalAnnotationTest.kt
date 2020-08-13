/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.externalAnnotations

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JavaModuleExternalPaths
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.*
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@TestRoot("idea/testData/externalAnnotations/")
@TestDataPath("\$CONTENT_ROOT")
@RunWith(JUnit38ClassRunner::class)
class ExternalAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getTestDataDirectory() = KotlinRoot.DIR

    fun testNotNullMethod() {
        KotlinTestUtils.runTest(::doTest, this, TargetBackend.ANY, "notNullMethod.kt")
    }

    fun testNullableMethod() {
        KotlinTestUtils.runTest(::doTest, this, TargetBackend.ANY, "nullableMethod.kt")
    }

    fun testNullableField() {
        KotlinTestUtils.runTest(::doTest, this, TargetBackend.ANY, "nullableField.kt")
    }

    fun testNullableMethodParameter() {
        KotlinTestUtils.runTest(::doTest, this, TargetBackend.ANY, "nullableMethodParameter.kt")
    }

    override fun setUp() {
        super.setUp()
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = true
        addFile(classWithExternalAnnotatedMembers)
    }

    override fun tearDown() {
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = false
        super.tearDown()
    }

    private fun addFile(path: String) {
        val file = File(getTestDataDirectory(), path)
        val root = LightPlatformTestCase.getSourceRoot()
        runWriteAction {
            val virtualFile = root.createChildData(null, file.name)
            virtualFile.getOutputStream(null).writer().use { it.write(FileUtil.loadFile(file)) }
        }
    }

    private fun doTest(kotlinFilePath: String) {
        myFixture.configureByFiles(kotlinFilePath, testPath(externalAnnotationsFile), testPath(classWithExternalAnnotatedMembers))
        myFixture.checkHighlighting()
    }

    override fun getProjectDescriptor() = object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)
            model.getModuleExtension(JavaModuleExternalPaths::class.java)
                .setExternalAnnotationUrls(arrayOf(VfsUtilCore.pathToUrl(testPath(externalAnnotationsPath))))
        }
    }

    companion object {
        private const val externalAnnotationsPath = "idea/testData/externalAnnotations/annotations/"
        private const val classWithExternalAnnotatedMembers = "idea/testData/externalAnnotations/ClassWithExternalAnnotatedMembers.java"
        private const val externalAnnotationsFile = "$externalAnnotationsPath/annotations.xml"
    }
}
