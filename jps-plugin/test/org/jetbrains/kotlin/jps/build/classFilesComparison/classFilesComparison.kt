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

package org.jetbrains.kotlin.jps.build.classFilesComparison

import com.google.common.collect.Sets
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.google.protobuf.ExtensionRegistry
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.jps.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.*
import org.jetbrains.kotlin.serialization.DebugProtoBuf
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.DebugJvmProtoBuf
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.io.ByteArrayInputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

// Set this to true if you want to dump all bytecode (test will fail in this case)
val DUMP_ALL = System.getProperty("comparison.dump.all") == "true"

fun File.hash() = Files.hash(this, Hashing.crc32())

fun getDirectoryString(dir: File, interestingPaths: List<String>): String {
    val buf = StringBuilder()
    val p = Printer(buf)


    fun addDirContent(dir: File) {
        p.pushIndent()

        val listFiles = dir.listFiles()
        assertNotNull(listFiles)

        val children = listFiles!!.sortedWith(compareBy ({ it.isDirectory }, { it.name } ))
        for (child in children) {
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

fun getAllRelativePaths(dir: File): Set<String> {
    val result = HashSet<String>()
    FileUtil.processFilesRecursively(dir) {
        if (it!!.isFile()) {
            result.add(FileUtil.getRelativePath(dir, it)!!)
        }

        true
    }

    return result
}

fun assertEqualDirectories(expected: File, actual: File, forgiveExtraFiles: Boolean) {
    val pathsInExpected = getAllRelativePaths(expected)
    val pathsInActual = getAllRelativePaths(actual)

    val commonPaths = Sets.intersection(pathsInExpected, pathsInActual)
    val changedPaths = commonPaths
            .filter { DUMP_ALL || !Arrays.equals(File(expected, it).readBytes(), File(actual, it).readBytes()) }
            .sorted()

    val expectedString = getDirectoryString(expected, changedPaths)
    val actualString = getDirectoryString(actual, changedPaths)

    if (DUMP_ALL) {
        assertEquals(expectedString, actualString + " ")
    }

    if (forgiveExtraFiles) {
        // If compilation fails, output may be different for full rebuild and partial make. Parsing output (directory string) for simplicity.
        if (changedPaths.isEmpty()) {
            val expectedListingLines = expectedString.split('\n').toList()
            val actualListingLines = actualString.split('\n').toList()
            if (actualListingLines.containsAll(expectedListingLines)) {
                return
            }
        }
    }

    assertEquals(expectedString, actualString)
}

fun classFileToString(classFile: File): String {
    val out = StringWriter()

    val traceVisitor = TraceClassVisitor(PrintWriter(out))
    ClassReader(classFile.readBytes()).accept(traceVisitor, 0)

    val classHeader = LocalFileKotlinClass.create(classFile)?.getClassHeader()

    val annotationDataEncoded = classHeader?.annotationData
    if (annotationDataEncoded != null) {
        ByteArrayInputStream(BitEncoding.decodeBytes(annotationDataEncoded)).use {
            input ->

            out.write("\n------ string table types proto -----\n${DebugJvmProtoBuf.StringTableTypes.parseDelimitedFrom(input)}")

            when {
                classHeader!!.isCompatiblePackageFacadeKind() ->
                    out.write("\n------ package proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                classHeader.isCompatibleFileFacadeKind() ->
                    out.write("\n------ file facade proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")
                classHeader.isCompatibleClassKind() ->
                    out.write("\n------ class proto -----\n${DebugProtoBuf.Class.parseFrom(input, getExtensionRegistry())}")
                classHeader.isCompatibleMultifileClassPartKind() ->
                    out.write("\n------ multi-file part proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")

                else -> throw IllegalStateException()
            }
        }
    }

    return out.toString()
}

fun getExtensionRegistry(): ExtensionRegistry {
    val registry = ExtensionRegistry.newInstance()!!
    DebugJvmProtoBuf.registerAllExtensions(registry)
    return registry
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
