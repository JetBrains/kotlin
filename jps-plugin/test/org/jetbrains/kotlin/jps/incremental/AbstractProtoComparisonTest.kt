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
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.Printer
import java.io.File

public abstract class AbstractProtoComparisonTest : UsefulTestCase() {

    public fun doTest(testDataPath: String) {
        val testDir = JetTestUtils.tmpDir("testDirectory")

        val oldClassFiles = compileFileAndGetClasses(testDataPath, testDir, "old.kt")
        val newClassFiles = compileFileAndGetClasses(testDataPath, testDir, "new.kt")

        val oldClassMap = oldClassFiles.toMap { it.name }
        val newClassMap = newClassFiles.toMap { it.name }

        val sb = StringBuilder()
        val p = Printer(sb)

        val oldSetOfNames = oldClassFiles.map { it.name }.toSet()
        val newSetOfNames = newClassFiles.map { it.name }.toSet()

        val removedNames = (oldSetOfNames - newSetOfNames).sorted()
        removedNames.forEach {
            p.println("REMOVED: class $it")
        }

        val addedNames = (newSetOfNames - oldSetOfNames).sorted()
        addedNames.forEach {
            p.println("ADDED: class $it")
        }

        val commonNames = oldSetOfNames.intersect(newSetOfNames).sorted()

        for(name in commonNames) {
            p.printDifference(oldClassMap[name]!!, newClassMap[name]!!)
        }

        JetTestUtils.assertEqualsToFile(File(testDataPath + File.separator + "result.out"), sb.toString());
    }

    private fun compileFileAndGetClasses(testPath: String, testDir: File, fileName: String): List<File> {

        val sourcesDirectory = testDir.createSubDirectory("sources")
        val classesDirectory = testDir.createSubDirectory("$fileName.src")

        FileUtil.copy(File(testPath, fileName), File(sourcesDirectory, "main.kt"))
        MockLibraryUtil.compileKotlin(sourcesDirectory.path, classesDirectory)

        return File(classesDirectory, "test").listFiles() { it.name.endsWith(".class") }?.sortedBy { it.name }!!
    }

    private fun Printer.printDifference(oldClassFile: File, newClassFile: File) {
        val oldLocalFileKotlinClass = LocalFileKotlinClass.create(oldClassFile)!!
        val newLocalFileKotlinClass = LocalFileKotlinClass.create(newClassFile)!!

        val oldClassHeader = oldLocalFileKotlinClass.classHeader
        val newClassHeader = newLocalFileKotlinClass.classHeader

        val oldProtoBytes = BitEncoding.decodeBytes(oldClassHeader.annotationData!!)
        val newProtoBytes = BitEncoding.decodeBytes(newClassHeader.annotationData!!)

        val oldProto = ProtoMapValue(
                oldClassHeader.isCompatiblePackageFacadeKind() || oldClassHeader.isCompatibleFileFacadeKind(),
                oldProtoBytes, oldClassHeader.strings!!
        )
        val newProto = ProtoMapValue(
                newClassHeader.isCompatiblePackageFacadeKind() || newClassHeader.isCompatibleFileFacadeKind(),
                newProtoBytes, newClassHeader.strings!!
        )

        val diff = when {
            newClassHeader.isCompatiblePackageFacadeKind(), newClassHeader.isCompatibleClassKind(), newClassHeader.isCompatibleFileFacadeKind() ->
                difference(oldProto, newProto)
            else ->  {
                println("ignore ${oldLocalFileKotlinClass.classId}")
                return
            }
        }

        val changes = when (diff) {
            is DifferenceKind.NONE ->
                "NONE"
            is DifferenceKind.CLASS_SIGNATURE ->
                "CLASS_SIGNATURE"
            is DifferenceKind.MEMBERS ->
                "MEMBERS\n    ${diff.names.sorted()}"
        }

        println("changes in ${oldLocalFileKotlinClass.classId}: $changes")
    }

    private fun File.createSubDirectory(relativePath: String): File {
        val directory = File(this, relativePath)
        FileUtil.createDirectory(directory)
        return directory
    }
}