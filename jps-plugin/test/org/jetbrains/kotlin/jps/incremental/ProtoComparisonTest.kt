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

package org.jetbrains.kotlin.jps.incremental

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import kotlin.test.assertTrue

public class ProtoComparisonTest : UsefulTestCase() {
    val TEST_DATA_PATH = "jps-plugin/testData/comparison"

    public fun testPrivateOnlyDifference() {
        doTest()
    }

    private fun doTest() {
        val testPath = TEST_DATA_PATH + File.separator + getTestName(true)

        val oldClassFiles = compileFileAndGetClasses(testPath, "old.kt")
        val newClassFiles = compileFileAndGetClasses(testPath, "new.kt")

        val sb = StringBuilder()
        val p = Printer(sb)

        val oldSetOfNames = oldClassFiles.map { it.getName() }.toSet()
        val newSetOfNames = newClassFiles.map { it.getName() }.toSet()
        assertTrue { oldSetOfNames == newSetOfNames }

        for(i in oldClassFiles.indices) {
            compareClasses(oldClassFiles[i], newClassFiles[i], p)
        }

        JetTestUtils.assertEqualsToFile(File(testPath + File.separator + "result.out"), sb.toString());
    }

    private fun compileFileAndGetClasses(testPath: String, fileName: String): List<File> {
        val testDir = JetTestUtils.tmpDir("testDirectory")
        val sourcesDirectory = testDir.createSubDirectory("sources")
        val cassesDirectory = testDir.createSubDirectory("classes")

        FileUtil.copy(File(testPath, fileName), File(sourcesDirectory, fileName))
        MockLibraryUtil.compileKotlin(sourcesDirectory.getPath(), cassesDirectory)

        return File(cassesDirectory, "test").listFiles() { it.getName().endsWith(".class") }?.sortBy { it.getName() }!!
    }

    private fun compareClasses(oldClassFile: File, newClassFile: File, p: Printer) {
        val oldLocalFileKotlinClass = LocalFileKotlinClass.create(oldClassFile)!!
        val newLocalFileKotlinClass = LocalFileKotlinClass.create(newClassFile)!!

        val oldClassHeader = oldLocalFileKotlinClass.classHeader
        val newClassHeader = newLocalFileKotlinClass.classHeader

        assertTrue { oldClassHeader.isCompatibleClassKind() }
        assertTrue { newClassHeader.isCompatibleClassKind() }

        assertTrue { oldClassHeader.classKind == JvmAnnotationNames.KotlinClass.Kind.CLASS }
        assertTrue { newClassHeader.classKind == JvmAnnotationNames.KotlinClass.Kind.CLASS }

        val oldProto = BitEncoding.decodeBytes(oldClassHeader.annotationData!!)
        val newProto = BitEncoding.decodeBytes(newClassHeader.annotationData!!)

        val res = if (isClassOpenPartNotChanged(oldProto, newProto)) "OK" else "Fail"

        p.println("private only changes in ${oldLocalFileKotlinClass.classId}: $res")
    }

    private fun File.createSubDirectory(relativePath: String): File {
        val directory = File(this, relativePath)
        FileUtil.createDirectory(directory)
        return directory
    }
}