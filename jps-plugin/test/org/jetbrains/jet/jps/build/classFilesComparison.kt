/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.build

import java.io.File
import org.junit.Assert.*
import org.jetbrains.asm4.util.TraceClassVisitor
import java.io.PrintWriter
import org.jetbrains.asm4.ClassReader
import java.io.StringWriter
import org.jetbrains.jet.utils.Printer
import com.google.common.io.Files
import com.google.common.hash.Hashing
import com.intellij.openapi.util.io.FileUtil
import com.google.common.collect.Sets
import java.util.HashSet
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass
import org.jetbrains.jet.storage.LockBasedStorageManager
import org.jetbrains.jet.lang.resolve.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.descriptors.serialization.BitEncoding
import org.jetbrains.jet.descriptors.serialization.DebugJavaProtoBuf
import com.google.protobuf.ExtensionRegistry
import java.io.ByteArrayInputStream
import org.jetbrains.jet.descriptors.serialization.DebugProtoBuf
import java.util.Arrays

fun File.hash() = Files.hash(this, Hashing.crc32())

fun getDirectoryString(dir: File, interestingPaths: List<String>, ignore: (File) -> Boolean): String {
    val buf = StringBuilder()
    val p = Printer(buf)


    fun addDirContent(dir: File) {
        p.pushIndent()

        val listFiles = dir.listFiles()
        assertNotNull(listFiles)

        val children = listFiles!!.toList().sortBy { it.getName() }.sortBy { it.isDirectory() }
        for (child in children) {
            if (ignore(child)) {
                continue
            }

            if (child.isDirectory()) {
                p.println(child.name)
                addDirContent(child)
            }
            else {
                p.println(child.name, " ", child.hash())
            }
        }

        p.popIndent()
    }


    p.println(".")
    addDirContent(dir)

    for (path in interestingPaths) {
        p.println("================", path, "================")
        p.println(fileToStringRepresentation(File(dir, path)))
        p.println()
        p.println()
    }

    return buf.toString()
}

fun getAllRelativePaths(dir: File, ignore: (File) -> Boolean): Set<String> {
    val result = HashSet<String>()
    FileUtil.processFilesRecursively(dir) {
        if (it!!.isFile() && !ignore(it)) {
            result.add(FileUtil.getRelativePath(dir, it)!!)
        }

        true
    }

    return result
}

fun assertEqualDirectories(expected: File, actual: File, ignore: (File) -> Boolean) {
    val pathsInExpected = getAllRelativePaths(expected, ignore)
    val pathsInActual = getAllRelativePaths(actual, ignore)

    val changedPaths = Sets.intersection(pathsInExpected, pathsInActual)
            .filter { !Arrays.equals(File(expected, it).readBytes(), File(actual, it).readBytes()) }
            .sort()

    val expectedString = getDirectoryString(expected, changedPaths, ignore)
    val actualString = getDirectoryString(actual, changedPaths, ignore)

    assertEquals(expectedString, actualString)
}

fun classFileToString(classFile: File): String {
    val out = StringWriter()
    val traceVisitor = TraceClassVisitor(PrintWriter(out))
    ClassReader(classFile.readBytes()).accept(traceVisitor, 0)

    // TODO serialize protobuf
    return out.toString()
}

fun fileToStringRepresentation(file: File): String {
    return when {
        file.name.endsWith(".class") -> {
            classFileToString(file)
        }
        else -> {
            file.readText()
        }
    }
}
