/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.*

/** Utility to serialize a [ClasspathSnapshot]. */
object ClasspathSnapshotSerializer {

    fun load(classpathEntrySnapshotFiles: List<File>): ClasspathSnapshot {
        return ClasspathSnapshot(classpathEntrySnapshotFiles.map {
            ClasspathEntrySnapshotSerializer.load(it)
        })
    }
}

object ClasspathEntrySnapshotSerializer : DataSerializer<ClasspathEntrySnapshot> {

    override fun save(output: DataOutput, snapshot: ClasspathEntrySnapshot) {
        LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotDataSerializer).save(output, snapshot.classSnapshots)
    }

    override fun read(input: DataInput): ClasspathEntrySnapshot {
        return ClasspathEntrySnapshot(
            classSnapshots = LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotDataSerializer).read(input)
        )
    }
}

object ClassSnapshotDataSerializer : DataSerializer<ClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ClassSnapshot) {
        output.writeString(snapshot.javaClass.name)
        when (snapshot) {
            is KotlinClassSnapshot -> KotlinClassSnapshotExternalizer.save(output, snapshot)
            is JavaClassSnapshot -> JavaClassSnapshotExternalizer.save(output, snapshot)
        }
    }

    override fun read(input: DataInput): ClassSnapshot {
        return when (val className = input.readString()) {
            KotlinClassSnapshot::class.java.name -> KotlinClassSnapshotExternalizer.read(input)
            JavaClassSnapshot::class.java.name -> JavaClassSnapshotExternalizer.read(input)
            else -> error("Unrecognized class: $className")
        }
    }
}

object KotlinClassSnapshotExternalizer : DataExternalizer<KotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: KotlinClassSnapshot) {
        KotlinClassInfoExternalizer.save(output, snapshot.classInfo)
    }

    override fun read(input: DataInput): KotlinClassSnapshot {
        return KotlinClassSnapshot(classInfo = KotlinClassInfoExternalizer.read(input))
    }
}

object KotlinClassInfoExternalizer : DataExternalizer<KotlinClassInfo> {

    override fun save(output: DataOutput, info: KotlinClassInfo) {
        ClassIdExternalizer.save(output, info.classId)
        output.writeInt(info.classKind.id)
        ListExternalizer(StringExternalizer).save(output, info.classHeaderData.toList())
        ListExternalizer(StringExternalizer).save(output, info.classHeaderStrings.toList())
        NullableValueExternalizer(StringExternalizer).save(output, info.multifileClassName)
        LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).save(output, info.constantsMap)
        LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).save(output, info.inlineFunctionsMap)
    }

    override fun read(input: DataInput): KotlinClassInfo {
        return KotlinClassInfo(
            classId = ClassIdExternalizer.read(input),
            classKind = KotlinClassHeader.Kind.getById(input.readInt()),
            classHeaderData = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            classHeaderStrings = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            multifileClassName = NullableValueExternalizer(StringExternalizer).read(input),
            constantsMap = LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).read(input),
            inlineFunctionsMap = LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).read(input)
        )
    }
}

object ClassIdExternalizer : DataExternalizer<ClassId> {

    override fun save(output: DataOutput, classId: ClassId) {
        FqNameExternalizer.save(output, classId.packageFqName)
        FqNameExternalizer.save(output, classId.relativeClassName)
        output.writeBoolean(classId.isLocal)
    }

    override fun read(input: DataInput): ClassId {
        return ClassId(
            /* packageFqName */ FqNameExternalizer.read(input),
            /* relativeClassName */ FqNameExternalizer.read(input),
            /* isLocal */ input.readBoolean()
        )
    }
}

object FqNameExternalizer : DataExternalizer<FqName> {

    override fun save(output: DataOutput, fqName: FqName) {
        output.writeString(fqName.asString())
    }

    override fun read(input: DataInput): FqName {
        return FqName(input.readString())
    }
}

object JavaClassSnapshotExternalizer : DataExternalizer<JavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassSnapshot) {
    }

    override fun read(input: DataInput): JavaClassSnapshot {
        return JavaClassSnapshot
    }
}

interface DataSerializer<T> : DataExternalizer<T> {

    fun save(file: File, value: T) {
        return FileOutputStream(file).buffered().use {
            it.writeValue(value)
        }
    }

    fun load(file: File): T {
        return FileInputStream(file).buffered().use {
            it.readValue()
        }
    }

    fun toByteArray(value: T): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.buffered().use {
            it.writeValue(value)
        }
        return byteArrayOutputStream.toByteArray()
    }

    fun fromByteArray(byteArray: ByteArray): T {
        return ByteArrayInputStream(byteArray).buffered().use {
            it.readValue()
        }
    }

    private fun OutputStream.writeValue(value: T) {
        DataOutputStream(this).use {
            save(it, value)
        }
    }

    private fun InputStream.readValue(): T {
        return DataInputStream(this).use {
            read(it)
        }
    }
}
