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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.idea.decompiler.builtIns.buildDecompiledTextForBuiltIns
import org.jetbrains.kotlin.idea.decompiler.builtIns.isInternalBuiltInFile
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClassFileDecompiler
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.junit.Assert

class BuiltInDecompilerConsistencyTest : KotlinLightCodeInsightFixtureTestCase() {
    private val classFileDecompiler = KotlinClassFileDecompiler()
    private val builtInsDecompiler = KotlinBuiltInDecompiler()

    fun testSameAsClsDecompilerForCompiledBuiltInClasses() {
        checkTextAndStubTreeMatchForClassesInDirectory(findDir("kotlin", project))
        checkTextAndStubTreeMatchForClassesInDirectory(findDir("kotlin.annotation", project))
        checkTextAndStubTreeMatchForClassesInDirectory(findDir("kotlin.reflect", project))
    }

    // check builtIn decompiler against classFile decompiler, assuming the latter is well tested
    // check for classes which are both compiled to jvm (.class) and serialized (.kotlin_class)
    // in IDE we would actually hide corresponding .kotlin_class files and show .class files only
    private fun checkTextAndStubTreeMatchForClassesInDirectory(dir: VirtualFile) {
        val groupedByExtension = dir.children.groupBy { it.extension }
        val classFiles = groupedByExtension["class"]!!.map { it.nameWithoutExtension }
        val kotlinClassFiles = groupedByExtension["kotlin_class"]!!.map { it.nameWithoutExtension }
        val intersection = classFiles.intersect(kotlinClassFiles)

        Assert.assertTrue("Some classes should be present", intersection.isNotEmpty())

        intersection.forEach { className ->
            val classFile = dir.findChild(className + ".class")!!
            val builtInFile = dir.findChild(className + ".kotlin_class")!!
            Assert.assertEquals(
                    "Text mismatch for $className", getText(classFile, classFileDecompiler),
                    buildDecompiledTextForBuiltIns(builtInFile).text // use internal api to avoid calling isInternalBuiltInFile
            )

            Assert.assertTrue(isInternalBuiltInFile(builtInFile))

            val classFileStub = classFileDecompiler.stubBuilder.buildFileStub(FileContentImpl.createByFile(classFile))!!
            // use internal api to avoid calling isInternalBuiltInFile
            val builtInFileStub = builtInsDecompiler.stubBuilder.doBuildFileStub(FileContentImpl.createByFile(builtInFile))!!
            Assert.assertEquals("Stub mismatch for $className", classFileStub.serializeToString(), builtInFileStub.serializeToString())
        }
    }

    private fun getText(file: VirtualFile, kotlinClassFileDecompiler: ClassFileDecompilers.Full): String {
        return kotlinClassFileDecompiler.createFileViewProvider(file, PsiManager.getInstance(project), false).document!!.text
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}


fun findDir(packageFqName: String, project: Project): VirtualFile {
    val classNameIndex = KotlinFullClassNameIndex.getInstance()
    val randomClassInPackage = classNameIndex.getAllKeys(project).find { it.startsWith(packageFqName + ".") && "." !in it.substringAfter(packageFqName + ".") }!!
    val classes = classNameIndex.get(randomClassInPackage, project, GlobalSearchScope.allScope(project))
    return classes.first().containingFile.virtualFile.parent!!
}