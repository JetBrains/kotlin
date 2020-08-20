/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.VfsTestUtil
import com.intellij.util.ThrowableRunnable
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.test.KotlinMultiModuleJava9ProjectDescriptor.ModuleDescriptor

abstract class KotlinLightJava9ModulesCodeInsightFixtureTestCase : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinMultiModuleJava9ProjectDescriptor

    override fun tearDown() {
        runAll(
            ThrowableRunnable { KotlinMultiModuleJava9ProjectDescriptor.cleanupSourceRoots() },
            ThrowableRunnable { super.tearDown() }
        )
    }

    protected fun addFile(path: String, text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN): VirtualFile =
        VfsTestUtil.createFile(module.root(), path, text)

    protected fun addKotlinFile(
        path: String,
        @Language("kotlin") text: String,
        module: ModuleDescriptor = ModuleDescriptor.MAIN
    ): VirtualFile = addFile(path, text.toTestData(), module)

    protected fun addJavaFile(path: String, @Language("java") text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN): VirtualFile =
        addFile(path, text.toTestData(), module)

    protected fun moduleInfo(@Language("JAVA") text: String, module: ModuleDescriptor = ModuleDescriptor.MAIN) =
        addFile("module-info.java", text.toTestData(), module)

    protected fun checkModuleInfo(@Language("JAVA") text: String) = myFixture.checkResult("module-info.java", text.toTestData(), false)
}

private const val IDENTIFIER_CARET = "CARET"
private const val COMMENT_CARET_CHAR = "/*|*/"
private const val COMMENT_CARET = "/*CARET*/"
private val ADDITIONAL_CARET_MARKERS = arrayOf(IDENTIFIER_CARET, COMMENT_CARET_CHAR, COMMENT_CARET)

private fun String.toTestData(): String =
    ADDITIONAL_CARET_MARKERS.fold(trimIndent()) { result, marker -> result.replace(marker, EditorTestUtil.CARET_TAG, ignoreCase = true) }