/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastFacade
import java.io.File

abstract class AbstractKotlinUastLightCodeInsightFixtureTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    override fun setUp() {
        super.setUp()
        Registry.get("kotlin.uast.multiresolve.enabled").setValue(true, testRootDisposable)
    }

    fun getVirtualFile(testName: String): VirtualFile {
        val testFile = TEST_KOTLIN_MODEL_DIR.listFiles { pathname -> pathname.nameWithoutExtension == testName }.first()
        val vfs = VirtualFileManager.getInstance().getFileSystem(URLUtil.FILE_PROTOCOL)
        return vfs.findFileByPath(testFile.canonicalPath)!!
    }

    abstract fun check(testName: String, file: UFile)

    fun doTest(testName: String, checkCallback: (String, UFile) -> Unit = { testName, file -> check(testName, file) }) {
        val virtualFile = getVirtualFile(testName)

        val psiFile = myFixture.configureByText(virtualFile.name, File(virtualFile.canonicalPath!!).readText())
        val uFile = UastFacade.convertElementWithParent(psiFile, null) ?: error("Can't get UFile for $testName")
        checkCallback(testName, uFile as UFile)
    }

}