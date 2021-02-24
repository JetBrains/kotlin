/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile

const val CLASS_STRUCTURE_ARTIFACT_TYPE = "class-structure"
private const val MODULE_INFO = "module-info.class"

abstract class StructureTransformAction : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        try {
            transform(inputArtifact.get().asFile, outputs)
        } catch (e: Throwable) {
            throw e
        }
    }
}

/**
 * [StructureTransformLegacyAction] is a legacy version of [StructureTransformAction] and should only be used when gradle version is 5.3
 * or less. The reason of having this legacy artifact transform is that declaring inputArtifact as type Provider<FileSystemLocation> is not
 * supported until gradle version 5.4. Once our minimal supported gradle version is 5.4 or above, this legacy artifact transform can be
 * removed.
 */
abstract class StructureTransformLegacyAction : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: File

    override fun transform(outputs: TransformOutputs) {
        try {
            transform(inputArtifact, outputs)
        } catch (e: Throwable) {
            throw e
        }
    }
}

private fun transform(input: File, outputs: TransformOutputs) {
    val data = if (input.isDirectory) {
        visitDirectory(input)
    } else {
        visitJar(input)
    }

    val dataFile = outputs.file("output.bin")
    data.saveTo(dataFile)
}

private fun visitDirectory(directory: File): ClasspathEntryData {
    val entryData = ClasspathEntryData()

    directory.walk().filter {
        it.extension == "class"
                && !it.relativeTo(directory).toString().toLowerCase().startsWith("meta-inf")
                && it.name != MODULE_INFO
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

            if (entry.name.endsWith("class")
                && !entry.name.toLowerCase().startsWith("meta-inf")
                && entry.name != MODULE_INFO
            ) {
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
        // Sort only classDependencies, as all keys in this map are keys of classAbiHash map.
        val sortedClassDependencies =
            classDependencies.toSortedMap().mapValues { ClassDependencies(it.value.abiTypes.sorted(), it.value.privateTypes.sorted()) }

        val names = LinkedHashMap<String, Int>()
        sortedClassDependencies.forEach {
            if (it.key !in names) {
                names[it.key] = names.size
            }
            it.value.abiTypes.forEach { type ->
                if (type !in names) names[type] = names.size
            }
            it.value.privateTypes.forEach { type ->
                if (type !in names) names[type] = names.size
            }
        }

        output.writeInt(names.size)
        names.forEach { (key, value) ->
            output.writeInt(value)
            output.writeUTF(key)
        }

        output.writeInt(classAbiHash.size)
        sortedClassDependencies.forEach { (key, _) ->
            output.writeInt(names[key]!!)
            classAbiHash[key]!!.let {
                output.writeInt(it.size)
                output.write(it)
            }
        }

        output.writeInt(sortedClassDependencies.size)
        sortedClassDependencies.forEach {
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