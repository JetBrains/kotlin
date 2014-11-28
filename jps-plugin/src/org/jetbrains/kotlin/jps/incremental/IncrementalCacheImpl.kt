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

import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl.RecompilationDecision.*
import java.io.File
import com.intellij.util.io.PersistentHashMap
import java.io.DataOutput
import com.intellij.util.io.IOUtil
import java.io.DataInput
import org.jetbrains.kotlin.name.FqName
import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import java.util.Arrays
import org.jetbrains.org.objectweb.asm.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.HashSet
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache
import java.util.HashMap
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import com.intellij.openapi.util.io.FileUtil
import java.security.MessageDigest
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.jps.builders.storage.StorageProvider
import java.io.IOException
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths

val INLINE_ANNOTATION_DESC = "Lkotlin/inline;"

private val CACHE_DIRECTORY_NAME = "kotlin"


class CacheFormatVersion(targetDataRoot: File) {
    class object {
        // Change this when incremental cache format changes
        private val INCREMENTAL_CACHE_OWN_VERSION = 1
        private val CACHE_FORMAT_VERSION: Int = INCREMENTAL_CACHE_OWN_VERSION * 1000000 + JvmAbi.VERSION
        val FORMAT_VERSION_FILE_PATH: String = "$CACHE_DIRECTORY_NAME/format-version.txt"
    }

    private val file = File(targetDataRoot, FORMAT_VERSION_FILE_PATH)

    public fun isIncompatible(): Boolean {
        if (!file.exists()) return false

        return file.readText().toInt() != CACHE_FORMAT_VERSION
    }

    fun saveIfNeeded() {
        if (!file.exists()) {
            file.writeText(CACHE_FORMAT_VERSION.toString())
        }
    }

    fun clean() {
        file.delete()
    }
}

public class IncrementalCacheImpl(targetDataRoot: File): StorageOwner, IncrementalCache {
    class object {
        val PROTO_MAP = "proto.tab"
        val CONSTANTS_MAP = "constants.tab"
        val INLINE_FUNCTIONS = "inline-functions.tab"
        val PACKAGE_PARTS = "package-parts.tab"
    }

    private val baseDir = File(targetDataRoot, CACHE_DIRECTORY_NAME)
    private val protoMap =  ProtoMap()
    private val constantsMap =  ConstantsMap()
    private val inlineFunctionsMap =  InlineFunctionsMap()
    private val packagePartMap =  PackagePartMap()

    private val maps = listOf(protoMap, constantsMap, inlineFunctionsMap, packagePartMap)

    private val cacheFormatVersion = CacheFormatVersion(targetDataRoot)

    public fun saveFileToCache(sourceFiles: Collection<File>, classFile: File): RecompilationDecision {
        if (classFile.extension.toLowerCase() != "class") return DO_NOTHING
        
        cacheFormatVersion.saveIfNeeded()

        val kotlinClass = LocalFileKotlinClass.create(classFile)
        if (kotlinClass == null) return DO_NOTHING

        val fileBytes = kotlinClass.getFileContents()
        val className = JvmClassName.byClassId(kotlinClass.getClassId())
        val header = kotlinClass.getClassHeader()

        val annotationDataEncoded = header.annotationData
        if (annotationDataEncoded != null) {
            val data = BitEncoding.decodeBytes(annotationDataEncoded)
            when {
                header.isCompatiblePackageFacadeKind() -> {
                    return if (protoMap.put(className, data)) RECOMPILE_OTHERS_IN_CHUNK else DO_NOTHING
                }
                header.isCompatibleClassKind() -> {
                    val inlinesChanged = inlineFunctionsMap.process(className, fileBytes)
                    val protoChanged = protoMap.put(className, data)
                    val constantsChanged = constantsMap.process(className, fileBytes)

                    return when {
                        inlinesChanged -> RECOMPILE_ALL_CHUNK_AND_DEPENDANTS
                        constantsChanged -> RECOMPILE_OTHERS_WITH_DEPENDANTS
                        protoChanged -> RECOMPILE_OTHERS_IN_CHUNK
                        else -> DO_NOTHING
                    }
                }
                else -> {
                    throw IllegalStateException("Unexpected kind with annotationData: ${header.kind}, isCompatible: ${header.isCompatibleAbiVersion}")
                }
            }
        }

        if (header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART) {
            assert(sourceFiles.size == 1) { "Package part from several source files: $sourceFiles" }

            packagePartMap.putPackagePartSourceData(sourceFiles.first(), className)
            val inlinesChanged = inlineFunctionsMap.process(className, fileBytes)
            val constantsChanged = constantsMap.process(className, fileBytes)

            return when {
                inlinesChanged -> RECOMPILE_ALL_CHUNK_AND_DEPENDANTS
                constantsChanged -> RECOMPILE_OTHERS_WITH_DEPENDANTS
                else -> DO_NOTHING
            }
        }

        return DO_NOTHING
    }

    public fun clearCacheForRemovedFiles(removedSourceFiles: Collection<File>, outDirectory: File, compilationSuccessful: Boolean) {
        removedSourceFiles.forEach { packagePartMap.remove(it) }

        if (compilationSuccessful) {
            inlineFunctionsMap.clearOutdated(outDirectory)
            constantsMap.clearOutdated(outDirectory)
            protoMap.clearOutdated(outDirectory)
        }
    }

    public override fun getRemovedPackageParts(sourceFilesToCompileAndFqNames: Map<File, String?>): Collection<String> {
        return packagePartMap.getRemovedPackageParts(sourceFilesToCompileAndFqNames)
    }

    public override fun getPackageData(fqName: String): ByteArray? {
        return protoMap[JvmClassName.byFqNameWithoutInnerClasses(PackageClassUtils.getPackageClassFqName(FqName(fqName)))]
    }

    override fun flush(memoryCachesOnly: Boolean) {
        maps.forEach { it.flush(memoryCachesOnly) }
    }

    public override fun clean() {
        maps.forEach { it.clean() }
        cacheFormatVersion.clean()
    }

    public override fun close() {
        maps.forEach { it.close () }
    }

    private abstract class BasicMap<V> {
        protected var storage: PersistentHashMap<String, V> = createMap()

        protected abstract fun createMap(): PersistentHashMap<String, V>

        public fun clean() {
            try {
                storage.close()
            }
            catch (ignored: IOException) {
            }

            PersistentHashMap.deleteFilesStartingWith(storage.getBaseFile()!!)
            try {
                storage = createMap()
            }
            catch (ignored: IOException) {
            }
        }

        public fun flush(memoryCachesOnly: Boolean) {
            if (memoryCachesOnly) {
                if (storage.isDirty()) {
                    storage.dropMemoryCaches()
                }
            }
            else {
                storage.force()
            }
        }

        public fun close() {
            storage.close()
        }
    }

    private abstract class ClassFileBasedMap<V>: BasicMap<V>() {

        // TODO may be too expensive, because it traverses all files in out directory
        public fun clearOutdated(outDirectory: File) {
            val keysToRemove = HashSet<String>()

            storage.processKeysWithExistingMapping { key ->
                val className = JvmClassName.byInternalName(key!!)
                val classFile = File(outDirectory, FileUtil.toSystemDependentName(className.getInternalName()) + ".class")
                if (!classFile.exists()) {
                    keysToRemove.add(key)
                }

                true
            }

            for (key in keysToRemove) {
                storage.remove(key)
            }
        }
    }

    private inner class ProtoMap: ClassFileBasedMap<ByteArray>() {
        override fun createMap(): PersistentHashMap<String, ByteArray> = PersistentHashMap(
                File(baseDir, PROTO_MAP),
                EnumeratorStringDescriptor(),
                ByteArrayExternalizer
        )

        public fun put(className: JvmClassName, data: ByteArray): Boolean {
            val key = className.getInternalName()
            val oldData = storage[key]
            if (Arrays.equals(data, oldData)) {
                return false
            }
            storage.put(key, data)
            return true
        }

        public fun get(className: JvmClassName): ByteArray? {
            return storage[className.getInternalName()]
        }
    }

    private inner class ConstantsMap: ClassFileBasedMap<Map<String, Any>>() {
        override fun createMap(): PersistentHashMap<String, Map<String, Any>> = PersistentHashMap(
                File(baseDir, CONSTANTS_MAP),
                EnumeratorStringDescriptor(),
                ConstantsMapExternalizer
        )

        private fun getConstantsMap(bytes: ByteArray): Map<String, Any>? {
            val result = HashMap<String, Any>()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                    val staticFinal = Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
                    if (value != null && access and staticFinal == staticFinal) {
                        result[name] = value
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            return if (result.isEmpty()) null else result
        }

        public fun process(className: JvmClassName, bytes: ByteArray): Boolean {
            return put(className, getConstantsMap(bytes))
        }

        private fun put(className: JvmClassName, constantsMap: Map<String, Any>?): Boolean {
            val key = className.getInternalName()

            val oldMap = storage[key]
            if (oldMap == constantsMap) {
                return false
            }
            if (constantsMap != null) {
                storage.put(key, constantsMap)
            }
            else {
                storage.remove(key)
            }
            return true
        }
    }

    private object ConstantsMapExternalizer: DataExternalizer<Map<String, Any>> {
        override fun save(out: DataOutput, map: Map<String, Any>?) {
            out.writeInt(map!!.size)
            for (name in map.keySet().toSortedList()) {
                IOUtil.writeString(name, out)
                val value = map[name]!!
                when (value) {
                    is Int -> {
                        out.writeByte(Kind.INT.ordinal())
                        out.writeInt(value)
                    }
                    is Float -> {
                        out.writeByte(Kind.FLOAT.ordinal())
                        out.writeFloat(value)
                    }
                    is Long -> {
                        out.writeByte(Kind.LONG.ordinal())
                        out.writeLong(value)
                    }
                    is Double -> {
                        out.writeByte(Kind.DOUBLE.ordinal())
                        out.writeDouble(value)
                    }
                    is String -> {
                        out.writeByte(Kind.STRING.ordinal())
                        IOUtil.writeString(value, out)
                    }
                    else -> throw IllegalStateException("Unexpected constant class: ${value.javaClass}")
                }
            }
        }

        override fun read(`in`: DataInput): Map<String, Any>? {
            val size = `in`.readInt()
            val map = HashMap<String, Any>(size)

            for (i in size.indices) {
                val name = IOUtil.readString(`in`)!!

                val kind = Kind.values()[`in`.readByte().toInt()]
                val value = when (kind) {
                    Kind.INT -> `in`.readInt()
                    Kind.FLOAT -> `in`.readFloat()
                    Kind.LONG -> `in`.readLong()
                    Kind.DOUBLE -> `in`.readDouble()
                    Kind.STRING -> IOUtil.readString(`in`)!!
                }
                map[name] = value
            }

            return map
        }

        private enum class Kind {
            INT FLOAT LONG DOUBLE STRING
        }
    }

    private inner class InlineFunctionsMap: ClassFileBasedMap<Map<String, Long>>() {
        override fun createMap(): PersistentHashMap<String, Map<String, Long>> = PersistentHashMap(
                File(baseDir, INLINE_FUNCTIONS),
                EnumeratorStringDescriptor(),
                InlineFunctionsMapExternalizer
        )

        private fun getInlineFunctionsMap(bytes: ByteArray): Map<String, Long>? {
            val result = HashMap<String, Long>()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val dummyClassWriter = ClassWriter(Opcodes.ASM5)
                    return object : MethodVisitor(Opcodes.ASM5, dummyClassWriter.visitMethod(0, name, desc, null, exceptions)) {
                        var hasInlineAnnotation = false

                        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                            if (desc == INLINE_ANNOTATION_DESC) {
                                hasInlineAnnotation = true
                            }
                            return null
                        }

                        override fun visitEnd() {
                            if (hasInlineAnnotation) {
                                val dummyBytes = dummyClassWriter.toByteArray()!!
                                val hash = dummyBytes.md5()

                                result[name + desc] = hash
                            }
                        }
                    }
                }

            }, 0)

            return if (result.isEmpty()) null else result
        }

        public fun process(className: JvmClassName, bytes: ByteArray): Boolean {
            return put(className, getInlineFunctionsMap(bytes))
        }

        private fun put(className: JvmClassName, inlineFunctionsMap: Map<String, Long>?): Boolean {
            val key = className.getInternalName()

            val oldMap = storage[key]
            if (oldMap == inlineFunctionsMap) {
                return false
            }
            if (inlineFunctionsMap != null) {
                storage.put(key, inlineFunctionsMap)
            }
            else {
                storage.remove(key)
            }
            return true
        }
    }

    private object InlineFunctionsMapExternalizer: DataExternalizer<Map<String, Long>> {
        override fun save(out: DataOutput, map: Map<String, Long>?) {
            out.writeInt(map!!.size)
            for (name in map.keySet()) {
                IOUtil.writeString(name, out)
                out.writeLong(map[name]!!)
            }
        }

        override fun read(`in`: DataInput): Map<String, Long>? {
            val size = `in`.readInt()
            val map = HashMap<String, Long>(size)

            for (i in size.indices) {
                val name = IOUtil.readString(`in`)!!
                val value = `in`.readLong()

                map[name] = value
            }

            return map
        }

    }


    private inner class PackagePartMap: BasicMap<String>() {
        // Format of serialization to string: <source file path>  -->  <package part JVM internal name>
        override fun createMap(): PersistentHashMap<String, String> = PersistentHashMap(
                File(baseDir, PACKAGE_PARTS),
                EnumeratorStringDescriptor(),
                EnumeratorStringDescriptor()
        )

        public fun putPackagePartSourceData(sourceFile: File, className: JvmClassName) {
            storage.put(sourceFile.getAbsolutePath(), className.getInternalName())
        }

        public fun remove(sourceFile: File) {
            storage.remove(sourceFile.getAbsolutePath())
        }

        public fun getRemovedPackageParts(compiledSourceFilesToFqName: Map<File, String?>): Collection<String> {
            val result = HashSet<String>()

            storage.processKeysWithExistingMapping { key ->
                val sourceFile = File(key!!)

                val packagePartClassName = storage[key]!!
                if (!sourceFile.exists()) {
                    result.add(packagePartClassName)
                }
                else {
                    if (sourceFile in compiledSourceFilesToFqName) {
                        val previousPackageFqName = JvmClassName.byInternalName(packagePartClassName).getPackageFqName()
                        if (compiledSourceFilesToFqName[sourceFile] != previousPackageFqName.asString()) {
                            result.add(packagePartClassName)
                        }
                    }
                }

                true
            }

            return result
        }

        public fun getPackages(): Set<FqName> {
            val result = HashSet<FqName>()

            storage.processKeysWithExistingMapping { key ->
                val packagePartClassName = storage[key!!]!!

                val packageFqName = JvmClassName.byInternalName(packagePartClassName).getPackageFqName()

                result.add(packageFqName)

                true
            }

            return result
        }
    }

    enum class RecompilationDecision {
        DO_NOTHING
        RECOMPILE_OTHERS_IN_CHUNK
        RECOMPILE_OTHERS_WITH_DEPENDANTS
        RECOMPILE_ALL_CHUNK_AND_DEPENDANTS

        fun merge(other: RecompilationDecision): RecompilationDecision {
            return if (other.ordinal() > this.ordinal()) other else this
        }
    }
}

private val storageProvider = object: StorageProvider<IncrementalCacheImpl>() {
    override fun createStorage(targetDataDir: File): IncrementalCacheImpl {
        return IncrementalCacheImpl(targetDataDir)
    }
}

public fun BuildDataPaths.getKotlinCacheVersion(target: BuildTarget<*>): CacheFormatVersion = CacheFormatVersion(getTargetDataRoot(target))

public fun BuildDataManager.getKotlinCache(target: BuildTarget<*>): IncrementalCacheImpl = getStorage(target, storageProvider)

private fun ByteArray.md5(): Long {
    val d = MessageDigest.getInstance("MD5").digest(this)!!
    return ((d[0].toLong() and 0xFFL)
                    or ((d[1].toLong() and 0xFFL) shl 8)
                    or ((d[2].toLong() and 0xFFL) shl 16)
                    or ((d[3].toLong() and 0xFFL) shl 24)
                    or ((d[4].toLong() and 0xFFL) shl 32)
                    or ((d[5].toLong() and 0xFFL) shl 40)
                    or ((d[6].toLong() and 0xFFL) shl 48)
                    or ((d[7].toLong() and 0xFFL) shl 56)
            )
}

private object ByteArrayExternalizer: DataExternalizer<ByteArray> {
    override fun save(out: DataOutput, value: ByteArray) {
        out.writeInt(value.size())
        out.write(value)
    }

    override fun read(`in`: DataInput): ByteArray {
        val length = `in`.readInt()
        val buf = ByteArray(length)
        `in`.readFully(buf)
        return buf
    }
}
