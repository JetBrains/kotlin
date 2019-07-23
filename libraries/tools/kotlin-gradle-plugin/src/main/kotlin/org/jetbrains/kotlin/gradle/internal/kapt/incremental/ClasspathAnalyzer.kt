/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.gradle.api.artifacts.transform.ArtifactTransform
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile

const val CLASS_STRUCTURE_ARTIFACT_TYPE = "class-structure"

class StructureArtifactTransform : ArtifactTransform() {
    override fun transform(input: File): MutableList<File> {
        try {
            val data = if (input.isDirectory) {
                visitDirectory(input)
            } else {
                visitJar(input)
            }

            val dataFile = outputDirectory.resolve("output.bin")
            data.saveTo(dataFile)

            return mutableListOf(dataFile)
        } catch (e: Throwable) {
            throw e
        }
    }
}

private fun visitDirectory(directory: File): ClasspathEntryData {
    val entryData = ClasspathEntryData()

    directory.walk().filter {
        it.extension == "class" && !it.relativeTo(directory).toString().toLowerCase().startsWith("meta-inf")
    }.forEach {
        val internalName = it.relativeTo(directory).invariantSeparatorsPath.dropLast(".class".length)
        BufferedInputStream(it.inputStream()).use { inputStream ->
            analyzeInputStream(inputStream, internalName, entryData)
        }
    }

    return entryData
}

private fun visitJar(jar: File): ClasspathEntryData {
    val entryData = ClasspathEntryData()

    ZipFile(jar).use { zipFile ->
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()

            if (entry.name.endsWith("class") && !entry.name.toLowerCase().startsWith("meta-inf")) {
                BufferedInputStream(zipFile.getInputStream(entry)).use { inputStream ->
                    analyzeInputStream(inputStream, entry.name.dropLast(".class".length), entryData)
                }
            }
        }
    }

    return entryData
}

private fun analyzeInputStream(input: InputStream, internalName: String, entryData: ClasspathEntryData) {
    val abiExtractor = ClassAbiExtractor(ClassWriter(0))
    val typeDependenciesExtractor = ClassTypeExtractorVisitor(abiExtractor)
    ClassReader(input.readBytes()).accept(
        typeDependenciesExtractor,
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    )

    val bytes = abiExtractor.getBytes()
    val digest = MessageDigest.getInstance("MD5").digest(bytes)

    entryData.classAbiHash[internalName] = digest
    entryData.classDependencies[internalName] =
        ClassDependencies(typeDependenciesExtractor.getAbiTypes(), typeDependenciesExtractor.getPrivateTypes())
}

class ClasspathEntryData : Serializable {

    object ClasspathEntrySerializer {
        fun loadFrom(file: File): ClasspathEntryData {
            ObjectInputStream(BufferedInputStream(file.inputStream())).use {
                return it.readObject() as ClasspathEntryData
            }
        }
    }

    @Transient
    var classAbiHash = mutableMapOf<String, ByteArray>()
    @Transient
    var classDependencies = mutableMapOf<String, ClassDependencies>()

    private fun writeObject(output: ObjectOutputStream) {
        val names = mutableMapOf<String, Int>()
        classAbiHash.keys.forEach { names[it] = names.size }
        classDependencies.values.forEach {
            it.abiTypes.forEach {
                if (!names.containsKey(it)) names[it] = names.size
            }
            it.privateTypes.forEach {
                if (!names.containsKey(it)) names[it] = names.size
            }
        }

        output.writeInt(names.size)
        names.forEach { key, value ->
            output.writeInt(value)
            output.writeUTF(key)
        }

        output.writeInt(classAbiHash.size)
        classAbiHash.forEach {
            output.writeInt(names[it.key]!!)
            output.writeInt(it.value.size)
            output.write(it.value)
        }

        output.writeInt(classDependencies.size)
        classDependencies.forEach {
            output.writeInt(names[it.key]!!)

            output.writeInt(it.value.abiTypes.size)
            it.value.abiTypes.forEach {
                output.writeInt(names[it]!!)
            }

            output.writeInt(it.value.privateTypes.size)
            it.value.privateTypes.forEach {
                output.writeInt(names[it]!!)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {
        val namesSize = input.readInt()
        val names = HashMap<Int, String>(namesSize)
        repeat(namesSize) {
            val classId = input.readInt()
            val classInternalName = input.readUTF()
            names[classId] = classInternalName
        }

        val abiHashesSize = input.readInt()
        classAbiHash = HashMap(abiHashesSize)
        repeat(abiHashesSize) {
            val internalName = names[input.readInt()]!!
            val byteArraySize = input.readInt()
            val hash = ByteArray(byteArraySize)
            repeat(byteArraySize) {
                hash[it] = input.readByte()
            }
            classAbiHash[internalName] = hash
        }

        val dependenciesSize = input.readInt()
        classDependencies = HashMap(dependenciesSize)

        repeat(dependenciesSize) {
            val internalName = names[input.readInt()]!!

            val abiTypesSize = input.readInt()
            val abiTypeNames = HashSet<String>(abiTypesSize)
            repeat(abiTypesSize) {
                abiTypeNames.add(names[input.readInt()]!!)
            }

            val privateTypesSize = input.readInt()
            val privateTypeNames = HashSet<String>(privateTypesSize)
            repeat(privateTypesSize) {
                privateTypeNames.add(names[input.readInt()]!!)
            }

            classDependencies[internalName] = ClassDependencies(abiTypeNames, privateTypeNames)
        }
    }

    fun saveTo(file: File) {
        ObjectOutputStream(BufferedOutputStream(file.outputStream())).use {
            it.writeObject(this)
        }
    }
}

class ClassDependencies(val abiTypes: Collection<String>, val privateTypes: Collection<String>)