/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.JavaClassProtoMapValueExternalizer
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
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
        output.writeBoolean(snapshot is KotlinClassSnapshot)
        when (snapshot) {
            is KotlinClassSnapshot -> KotlinClassSnapshotExternalizer.save(output, snapshot)
            is JavaClassSnapshot -> JavaClassSnapshotExternalizer.save(output, snapshot)
        }
    }

    override fun read(input: DataInput): ClassSnapshot {
        val isKotlinClassSnapshot = input.readBoolean()
        return if (isKotlinClassSnapshot) {
            KotlinClassSnapshotExternalizer.read(input)
        } else {
            JavaClassSnapshotExternalizer.read(input)
        }
    }
}

object KotlinClassSnapshotExternalizer : DataExternalizer<KotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: KotlinClassSnapshot) {
        KotlinClassInfoExternalizer.save(output, snapshot.classInfo)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
    }

    override fun read(input: DataInput): KotlinClassSnapshot {
        return KotlinClassSnapshot(
            classInfo = KotlinClassInfoExternalizer.read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input)
        )
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

object JavaClassSnapshotExternalizer : DataExternalizer<JavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassSnapshot) {
        output.writeString(snapshot.javaClass.name)
        when (snapshot) {
            is RegularJavaClassSnapshot -> RegularJavaClassSnapshotExternalizer.save(output, snapshot)
            is ProtoBasedJavaClassSnapshot -> ProtoBasedJavaClassSnapshotExternalizer.save(output, snapshot)
            is EmptyJavaClassSnapshot -> EmptyJavaClassSnapshotExternalizer.save(output, snapshot)
            is ContentHashJavaClassSnapshot -> ContentHashJavaClassSnapshotExternalizer.save(output, snapshot)
        }
    }

    override fun read(input: DataInput): JavaClassSnapshot {
        return when (val className = input.readString()) {
            RegularJavaClassSnapshot::class.java.name -> RegularJavaClassSnapshotExternalizer.read(input)
            ProtoBasedJavaClassSnapshot::class.java.name -> ProtoBasedJavaClassSnapshotExternalizer.read(input)
            EmptyJavaClassSnapshot::class.java.name -> EmptyJavaClassSnapshotExternalizer.read(input)
            ContentHashJavaClassSnapshot::class.java.name -> ContentHashJavaClassSnapshotExternalizer.read(input)
            else -> error("Unrecognized class name: $className")
        }
    }
}

object RegularJavaClassSnapshotExternalizer : DataExternalizer<RegularJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: RegularJavaClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
        AbiSnapshotExternalizer.save(output, snapshot.classAbiExcludingMembers)
        ListExternalizer(AbiSnapshotExternalizer).save(output, snapshot.fieldsAbi)
        ListExternalizer(AbiSnapshotExternalizer).save(output, snapshot.methodsAbi)
    }

    override fun read(input: DataInput): RegularJavaClassSnapshot {
        return RegularJavaClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input),
            classAbiExcludingMembers = AbiSnapshotExternalizer.read(input),
            fieldsAbi = ListExternalizer(AbiSnapshotExternalizer).read(input),
            methodsAbi = ListExternalizer(AbiSnapshotExternalizer).read(input)
        )
    }
}

object AbiSnapshotExternalizer : DataExternalizer<AbiSnapshot> {

    override fun save(output: DataOutput, value: AbiSnapshot) {
        output.writeString(value.name)
        LongExternalizer.save(output, value.abiHash)
    }

    override fun read(input: DataInput): AbiSnapshot {
        return AbiSnapshot(name = input.readString(), abiHash = LongExternalizer.read(input))
    }
}

object ProtoBasedJavaClassSnapshotExternalizer : DataExternalizer<ProtoBasedJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ProtoBasedJavaClassSnapshot) {
        JavaClassProtoMapValueExternalizer.save(output, snapshot.serializedJavaClass)
    }

    override fun read(input: DataInput): ProtoBasedJavaClassSnapshot {
        return ProtoBasedJavaClassSnapshot(serializedJavaClass = JavaClassProtoMapValueExternalizer.read(input))
    }
}

object EmptyJavaClassSnapshotExternalizer : DataExternalizer<EmptyJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: EmptyJavaClassSnapshot) {
        // Nothing to save
    }

    override fun read(input: DataInput): EmptyJavaClassSnapshot {
        return EmptyJavaClassSnapshot
    }
}

object ContentHashJavaClassSnapshotExternalizer : DataExternalizer<ContentHashJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ContentHashJavaClassSnapshot) {
        LongExternalizer.save(output, snapshot.contentHash)
    }

    override fun read(input: DataInput): ContentHashJavaClassSnapshot {
        return ContentHashJavaClassSnapshot(contentHash = LongExternalizer.read(input))
    }
}

interface DataSerializer<T> : DataExternalizer<T> {

    fun save(file: File, value: T) {
        return DataOutputStream(FileOutputStream(file).buffered()).use {
            save(it, value)
        }
    }

    fun load(file: File): T {
        return DataInputStream(FileInputStream(file).buffered()).use {
            read(it)
        }
    }

    fun toByteArray(value: T): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream.buffered()).use {
            save(it, value)
        }
        return byteArrayOutputStream.toByteArray()
    }

    fun fromByteArray(byteArray: ByteArray): T {
        return DataInputStream(ByteArrayInputStream(byteArray).buffered()).use {
            read(it)
        }
    }
}
