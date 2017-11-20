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

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.decompiler.builtIns.BuiltInDefinitionFile
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClassFileDecompiler
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.elements.KtClassElementType
import org.junit.Assert

class BuiltInDecompilerConsistencyTest : KotlinLightCodeInsightFixtureTestCase() {
    private val classFileDecompiler = KotlinClassFileDecompiler()
    private val builtInsDecompiler = KotlinBuiltInDecompiler()

    override fun setUp() {
        super.setUp()
        BuiltInDefinitionFile.FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = false
    }

    override fun tearDown() {
        BuiltInDefinitionFile.FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
        super.tearDown()
    }

    fun testSameAsClsDecompilerForCompiledBuiltInClasses() {
        doTest("kotlin")
        doTest("kotlin.annotation")
        doTest("kotlin.collections")
        doTest("kotlin.ranges")
        doTest("kotlin.reflect")
    }

    // Check stubs for decompiled built-in classes against stubs for decompiled JVM class files, assuming the latter are well tested
    // Check only those classes, stubs for which are present in the stub for a decompiled .kotlin_builtins file
    private fun doTest(packageFqName: String) {
        val dir = findDir(packageFqName, project)
        val groupedByExtension = dir.children.groupBy { it.extension }
        val classFiles = groupedByExtension[JavaClassFileType.INSTANCE.defaultExtension]!!.map { it.nameWithoutExtension }
        val builtInsFile = groupedByExtension[BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION]!!.single()

        val builtInFileStub = builtInsDecompiler.stubBuilder.buildFileStub(FileContentImpl.createByFile(builtInsFile))!!

        val classesEncountered = arrayListOf<FqName>()

        for (className in classFiles) {
            val classFile = dir.findChild(className + "." + JavaClassFileType.INSTANCE.defaultExtension)!!
            val fileContent = FileContentImpl.createByFile(classFile)
            if (IDEKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(fileContent.file) == null) continue
            val fileStub = classFileDecompiler.stubBuilder.buildFileStub(fileContent) ?: continue
            val classStub = fileStub.findChildStubByType(KtClassElementType.getStubType(false)) as? KotlinClassStub ?: continue
            val classFqName = classStub.getFqName()!!
            val builtInClassStub = builtInFileStub.childrenStubs.firstOrNull {
                it is KotlinClassStub && it.getFqName() == classFqName
            } ?: continue
            Assert.assertEquals("Stub mismatch for $classFqName", classStub.serializeToString(), builtInClassStub.serializeToString())
            classesEncountered.add(classFqName)
        }

        Assert.assertTrue("Too few classes encountered in package $packageFqName: $classesEncountered", classesEncountered.size >= 5)
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

internal fun findDir(packageFqName: String, project: Project): VirtualFile {
    val classNameIndex = KotlinFullClassNameIndex.getInstance()
    val randomClassInPackage = classNameIndex.getAllKeys(project).first {
        it.startsWith(packageFqName + ".") && "." !in it.substringAfter(packageFqName + ".")
    }
    val classes = classNameIndex.get(randomClassInPackage, project, GlobalSearchScope.allScope(project))
    val firstClass = classes.firstOrNull() ?: error("No classes with this name found: $randomClassInPackage (package name $packageFqName)")
    return firstClass.containingFile.virtualFile.parent
}
