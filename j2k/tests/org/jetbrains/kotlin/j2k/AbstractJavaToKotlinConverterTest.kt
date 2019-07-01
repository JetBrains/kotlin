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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.idea.caches.PerModulePackageCacheService.Companion.DEBUG_LOG_ENABLE_PerModulePackageCache
import java.io.File

abstract class AbstractJavaToKotlinConverterTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        project.DEBUG_LOG_ENABLE_PerModulePackageCache = true

        val testName = getTestName(false)
        if (testName.contains("Java8") || testName.contains("java8")) {
            LanguageLevelProjectExtension.getInstance(project).languageLevel = LanguageLevel.JDK_1_8
        }

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        invalidateLibraryCache(project)

        addFile("KotlinApi.kt", "kotlinApi")
        addFile("JavaApi.java", "javaApi")
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())

        project.DEBUG_LOG_ENABLE_PerModulePackageCache = false
        super.tearDown()
    }
    
    private fun addFile(fileName: String, dirName: String) {
        addFile(File("j2k/testData/$fileName"), dirName)
    }

    protected fun addFile(file: File, dirName: String): VirtualFile {
        return addFile(FileUtil.loadFile(file, true), file.name, dirName)
    }

    protected fun addFile(text: String, fileName: String, dirName: String): VirtualFile {
        return runWriteAction {
            val root = LightPlatformTestCase.getSourceRoot()!!
            val virtualDir = root.findChild(dirName) ?: root.createChildDirectory(null, dirName)
            val virtualFile = virtualDir.createChildData(null, fileName)
            virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
            virtualFile
        }
    }

    protected fun deleteFile(virtualFile: VirtualFile) {
        runWriteAction { virtualFile.delete(this) }
    }
}

