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

package org.jetbrains.kotlin.idea.internal

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor
import org.jetbrains.kotlin.idea.test.configureAs
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File

public class KotlinJavaScriptRefreshLibraryTest : KotlinLightCodeInsightFixtureTestCase() {

    private val TEST_DATA_PATH = "idea/testData/internal"
    private val TEST_PACKAGE = "test"

    fun getVirtualFileFinder(): VirtualFileFinder =
            JsVirtualFileFinder.SERVICE.getInstance(project)

    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor.instance

    fun testRefreshLibrary() {
        val testPath = TEST_DATA_PATH + File.separator + getTestName(true)

        val oldLibSources = testPath + File.separator + "LibraryOld"
        val newLibSources = testPath + File.separator + "LibraryNew"

        val oldLibraryJar = MockLibraryUtil.compileJsLibraryToJar(oldLibSources, JdkAndMockLibraryProjectDescriptor.LIBRARY_NAME, false)
        val newLibraryJar = MockLibraryUtil.compileJsLibraryToJar(newLibSources, JdkAndMockLibraryProjectDescriptor.LIBRARY_NAME, false)

        myModule.configureAs(object: JdkAndMockLibraryProjectDescriptor(oldLibSources, false, true) {
            override fun configureModule(module: Module, model: ModifiableRootModel) {
                val jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(oldLibraryJar.absolutePath) + "!/"

                val libraryModel = model.moduleLibraryTable.modifiableModel.createLibrary(JdkAndMockLibraryProjectDescriptor.LIBRARY_NAME).modifiableModel
                libraryModel.addRoot(jarUrl, OrderRootType.CLASSES)
                libraryModel.commit()
            }
        })

        KotlinJavaScriptLibraryManager.getInstance(project).syncUpdateProjectLibrary()

        val classIdA = ClassId(FqName(TEST_PACKAGE), FqName("A"), false)
        val classIdB = ClassId(FqName(TEST_PACKAGE), FqName("B"), false)
        val classIdC = ClassId(FqName(TEST_PACKAGE), FqName("C"), false)

        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdA) != null) { "old: expected not null for A"}
        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdB) != null) { "old: expected not null for B"}
        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdC) == null) { "old: expected null for C"}

        FileUtilRt.copy(newLibraryJar, oldLibraryJar)

        val libraryJarVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(oldLibraryJar)
        RefreshQueue.getInstance().refresh(/* async = */ false, /* recursive = */ true, /* finishRunnable = */ null, ModalityState.NON_MODAL, libraryJarVirtualFile)

        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdA) != null) { "new: expected not null for A"}
        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdB) == null) { "new: expected null for B"}
        assert(getVirtualFileFinder().findVirtualFileWithHeader(classIdC) != null) { "new: expected not null for C"}
    }
}