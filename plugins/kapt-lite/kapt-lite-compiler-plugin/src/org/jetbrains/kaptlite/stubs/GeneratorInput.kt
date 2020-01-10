/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

interface ClassFile {
    val internalName: String
    fun readClass(): ClassNode
}

interface GeneratorInput : Closeable {
    fun iterate(block: (ClassFile) -> Unit)
    fun find(internalName: String): ClassFile?
}

class CompiledFilesGeneratorInput(compiledFiles: List<ClassNode>) : GeneratorInput {
    private val compiledFiles: Map<String, ClassFile> = run {
        val map = LinkedHashMap<String, ClassFile>(compiledFiles.size)
        compiledFiles.forEach { map[it.name] = CompiledClassFile(it) }
        return@run map
    }

    override fun iterate(block: (ClassFile) -> Unit) {
        compiledFiles.values.forEach(block)
    }

    override fun find(internalName: String) = compiledFiles[internalName]

    override fun close() {}

    private class CompiledClassFile(private val classNode: ClassNode) : ClassFile {
        override val internalName: String = classNode.name
        override fun readClass() = classNode
    }
}

abstract class ByteClassFile : ClassFile {
    abstract fun readBytes(): ByteArray

    override fun readClass(): ClassNode {
        val bytes = readBytes()
        val classNode = ClassNode()
        ClassReader(bytes).accept(classNode, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
        return classNode
    }
}

class DirectoryGeneratorInput(private val dir: File) : GeneratorInput {
    override fun iterate(block: (ClassFile) -> Unit) {
        for (file in dir.walk()) {
            if (!file.isFile) {
                continue
            }

            val path = file.toRelativeString(dir)
            if (!path.endsWith(".class")) {
                continue
            }

            val internalName = path.dropLast(".class".length)
            block(RealClassFile(internalName, file))
        }
    }

    override fun find(internalName: String): ClassFile? {
        val file = File(dir, "$internalName.class")
        if (file.exists()) {
            return RealClassFile(internalName, file)
        }

        return null
    }

    override fun close() {}

    private class RealClassFile(override val internalName: String, val file: File) : ByteClassFile() {
        override fun readBytes() = file.readBytes()
    }
}

class JarGeneratorInput(file: File) : GeneratorInput {
    private val zipFile = ZipFile(file)

    override fun iterate(block: (ClassFile) -> Unit) {
        for (entry in zipFile.entries()) {
            if (entry.isDirectory) {
                continue
            }

            val name = entry.name
            if (!name.endsWith(".class")) {
                continue
            }

            val internalName = name.dropLast(".class".length)
            block(ZipEntryClassFile(internalName, entry))
        }
    }

    override fun find(internalName: String): ClassFile? {
        val entry = zipFile.getEntry("$internalName.class") ?: return null
        return ZipEntryClassFile(internalName, entry)
    }

    override fun close() {
        zipFile.close()
    }

    private inner class ZipEntryClassFile(override val internalName: String, private val entry: ZipEntry) : ByteClassFile() {
        override fun readBytes(): ByteArray {
            return zipFile.getInputStream(entry).use { it.readBytes() }
        }
    }
}