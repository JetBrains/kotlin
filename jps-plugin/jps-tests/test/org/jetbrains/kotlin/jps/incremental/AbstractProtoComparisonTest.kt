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
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.incremental.difference
import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
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


        val oldClassMap = oldClassFiles.map { LocalFileKotlinClass.create(it)!!.let { it.classId to it } }.toMap()
        val newClassMap = newClassFiles.map { LocalFileKotlinClass.create(it)!!.let { it.classId to it } }.toMap()

        val sb = StringBuilder()
        val p = Printer(sb)

        val oldSetOfNames = oldClassFiles.map { it.name }.toSet()
        val newSetOfNames = newClassFiles.map { it.name }.toSet()

        val removedNames = (oldClassMap.keys - newClassMap.keys).map { it.toString() }.sorted()
        removedNames.forEach {
            p.println("REMOVED: class $it")
        }

        val addedNames = (newClassMap.keys - oldClassMap.keys).map { it.toString() }.sorted()
        addedNames.forEach {
            p.println("ADDED: class $it")
        }

        val commonNames = oldClassMap.keys.intersect(newClassMap.keys).sortedBy { it.toString() }

        for(name in commonNames) {
            p.printDifference(oldClassMap[name]!!, newClassMap[name]!!)
        }

        KotlinTestUtils.assertEqualsToFile(File(testDataPath + File.separator + "result.out"), sb.toString())
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

    private fun Printer.printDifference(oldClass: LocalFileKotlinClass, newClass: LocalFileKotlinClass) {
        fun KotlinJvmBinaryClass.readProto(): ProtoMapValue? {
            assert(classHeader.metadataVersion.isCompatible()) { "Incompatible class ($classHeader): $location" }

            val bytes by lazy { BitEncoding.decodeBytes(classHeader.data!!) }
            val strings by lazy { classHeader.strings!! }

            return when (classHeader.kind) {
                KotlinClassHeader.Kind.CLASS -> {
                    ProtoMapValue(false, bytes, strings)
                }
                KotlinClassHeader.Kind.FILE_FACADE,
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    ProtoMapValue(true, bytes, strings)
                }
                else -> {
                    println("skip $classId")
                    null
                }
            }
        }

        val diff = difference(oldClass.readProto() ?: return,
                              newClass.readProto() ?: return)

        val changes = SmartList<String>()

        if (diff.isClassAffected) {
            changes.add("CLASS_SIGNATURE")
        }

        if (diff.changedMembersNames.isNotEmpty()) {
            changes.add("MEMBERS\n    ${diff.changedMembersNames.sorted()}")
        }

        if (changes.isEmpty()) {
            changes.add("NONE")
        }

        println("changes in ${oldClass.classId}: ${changes.joinToString()}")
    }

    private fun File.createSubDirectory(relativePath: String): File {
        val directory = File(this, relativePath)
        FileUtil.createDirectory(directory)
        return directory
    }
}
