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
import com.intellij.util.SmartList
import org.jetbrains.kotlin.jps.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.Printer
import java.io.File

abstract class AbstractProtoComparisonTest : UsefulTestCase() {

    fun doTest(testDataPath: String) {
        val testDir = KotlinTestUtils.tmpDir("testDirectory")

        val oldClassFiles = compileFileAndGetClasses(testDataPath, testDir, "old")
        val newClassFiles = compileFileAndGetClasses(testDataPath, testDir, "new")

        val oldClassMap = oldClassFiles.toMapBy { it.name }
        val newClassMap = newClassFiles.toMapBy { it.name }

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

        KotlinTestUtils.assertEqualsToFile(File(testDataPath + File.separator + "result.out"), sb.toString());
    }

    private fun compileFileAndGetClasses(testPath: String, testDir: File, prefix: String): List<File> {
        val files = File(testPath).listFiles { it -> it.name.startsWith(prefix) }!!
        val sourcesDirectory = testDir.createSubDirectory("sources")
        val classesDirectory = testDir.createSubDirectory("$prefix.src")

        files.forEach { file ->
            FileUtil.copy(file, File(sourcesDirectory, file.name.replaceFirst(prefix, "main")))
        }
        MockLibraryUtil.compileKotlin(sourcesDirectory.path, classesDirectory)

        return File(classesDirectory, "test").listFiles() { it -> it.name.endsWith(".class") }?.sortedBy { it.name }!!
    }

    private fun Printer.printDifference(oldClassFile: File, newClassFile: File) {
        val oldLocalFileKotlinClass = LocalFileKotlinClass.create(oldClassFile)!!
        val newLocalFileKotlinClass = LocalFileKotlinClass.create(newClassFile)!!

        val oldClassHeader = oldLocalFileKotlinClass.classHeader
        val newClassHeader = newLocalFileKotlinClass.classHeader

        if (oldClassHeader.annotationData == null || newClassHeader.annotationData == null) {
            println("skip ${oldLocalFileKotlinClass.classId}")
            return
        }

        val oldProtoBytes = BitEncoding.decodeBytes(oldClassHeader.annotationData!!)
        val newProtoBytes = BitEncoding.decodeBytes(newClassHeader.annotationData!!)

        assert(oldClassHeader.metadataVersion.isCompatible()) { "Incompatible class ($oldClassHeader): $oldClassFile" }
        assert(newClassHeader.metadataVersion.isCompatible()) { "Incompatible class ($newClassHeader): $newClassFile" }

        val oldProto = ProtoMapValue(
                oldClassHeader.kind == KotlinClassHeader.Kind.FILE_FACADE ||
                oldClassHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART,
                oldProtoBytes, oldClassHeader.strings!!
        )
        val newProto = ProtoMapValue(
                newClassHeader.kind == KotlinClassHeader.Kind.FILE_FACADE ||
                newClassHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART,
                newProtoBytes, newClassHeader.strings!!
        )

        val diff = when (newClassHeader.kind) {
            KotlinClassHeader.Kind.CLASS, KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ->
                difference(oldProto, newProto)
            else ->  {
                println("ignore ${oldLocalFileKotlinClass.classId}")
                return
            }
        }

        val changes = SmartList<String>()

        if (diff.isClassSignatureChanged) {
            changes.add("CLASS_SIGNATURE")
        }

        if (diff.changedMembersNames.isNotEmpty()) {
            changes.add("MEMBERS\n    ${diff.changedMembersNames.sorted()}")
        }

        if (changes.isEmpty()) {
            changes.add("NONE")
        }

        println("changes in ${oldLocalFileKotlinClass.classId}: ${changes.joinToString()}")
    }

    private fun File.createSubDirectory(relativePath: String): File {
        val directory = File(this, relativePath)
        FileUtil.createDirectory(directory)
        return directory
    }
}