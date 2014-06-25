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
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import org.jetbrains.org.objectweb.asm.ClassReader
import java.io.StringWriter
import org.jetbrains.jet.utils.Printer
import com.google.common.io.Files
import com.google.common.hash.Hashing
import com.intellij.openapi.util.io.FileUtil
import com.google.common.collect.Sets
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.descriptors.serialization.BitEncoding
import org.jetbrains.jet.descriptors.serialization.DebugJavaProtoBuf
import com.google.protobuf.ExtensionRegistry
import java.io.ByteArrayInputStream
import org.jetbrains.jet.descriptors.serialization.DebugProtoBuf
import java.util.Arrays
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass

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

        val children = listFiles!!.toList().sortBy { it.getName() }.sortBy { it.isDirectory() }
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

fun assertEqualDirectories(expected: File, actual: File) {
    val pathsInExpected = getAllRelativePaths(expected)
    val pathsInActual = getAllRelativePaths(actual)

    val commonPaths = Sets.intersection(pathsInExpected, pathsInActual)
    val changedPaths = commonPaths
            .filter { DUMP_ALL || !Arrays.equals(File(expected, it).readBytes(), File(actual, it).readBytes()) }
            .sort()

    val expectedString = getDirectoryString(expected, changedPaths)
    val actualString = getDirectoryString(actual, changedPaths)

    if (DUMP_ALL) {
        assertEquals(expectedString, actualString + " ")
    }

    assertEquals(expectedString, actualString)
}

fun classFileToString(classFile: File): String {
    val out = StringWriter()

    val classBytes = classFile.readBytes()

    val traceVisitor = TraceClassVisitor(PrintWriter(out))
    ClassReader(classBytes).accept(traceVisitor, 0)

    val classHeader = VirtualFileKotlinClass.readClassHeader(classBytes)

    val annotationDataEncoded = classHeader?.annotationData
    if (annotationDataEncoded != null) {
        ByteArrayInputStream(BitEncoding.decodeBytes(annotationDataEncoded)).use {
            input ->

        out.write("\n------ simpleNames proto -----\n${DebugProtoBuf.SimpleNameTable.parseDelimitedFrom(input)}")
        out.write("\n------ qualifiedNames proto -----\n${DebugProtoBuf.QualifiedNameTable.parseDelimitedFrom(input)}")

        when (classHeader!!.kind) {
            KotlinClassHeader.Kind.PACKAGE_FACADE ->
                out.write("\n------ package proto -----\n${DebugProtoBuf.Package.parseFrom(input, getExtensionRegistry())}")

                KotlinClassHeader.Kind.CLASS ->
                    out.write("\n------ class proto -----\n${DebugProtoBuf.Class.parseFrom(input, getExtensionRegistry())}")
                else -> throw IllegalStateException()
            }
        }
    }

    return out.toString()
}

fun getExtensionRegistry(): ExtensionRegistry {
    val registry = ExtensionRegistry.newInstance()!!
    DebugJavaProtoBuf.registerAllExtensions(registry)
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
