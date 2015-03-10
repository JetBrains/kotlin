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
import org.jetbrains.kotlin.load.kotlin.incremental.cache.IncrementalCache
import java.util.HashMap
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
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
import com.intellij.util.io.BooleanDataDescriptor
import java.util.ArrayList
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.utils.Printer
import java.io.DataInputStream

val INLINE_ANNOTATION_DESC = "Lkotlin/inline;"

private val CACHE_DIRECTORY_NAME = "kotlin"


class CacheFormatVersion(targetDataRoot: File) {
    default object {
        // Change this when incremental cache format changes
        private val INCREMENTAL_CACHE_OWN_VERSION = 2
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

public class IncrementalCacheImpl(targetDataRoot: File) : StorageOwner, IncrementalCache {
    default object {
        val PROTO_MAP = "proto.tab"
        val CONSTANTS_MAP = "constants.tab"
        val INLINE_FUNCTIONS = "inline-functions.tab"
        val PACKAGE_PARTS = "package-parts.tab"
        val SOURCE_TO_CLASSES = "source-to-classes.tab"
        val DIRTY_OUTPUT_CLASSES = "dirty-output-classes.tab"
    }

    private val baseDir = File(targetDataRoot, CACHE_DIRECTORY_NAME)
    private val protoMap = ProtoMap()
    private val constantsMap = ConstantsMap()
    private val inlineFunctionsMap = InlineFunctionsMap()
    private val packagePartMap = PackagePartMap()
    private val sourceToClassesMap = SourceToClassesMap()
    private val dirtyOutputClassesMap = DirtyOutputClassesMap()

    private val maps = listOf(protoMap, constantsMap, inlineFunctionsMap, packagePartMap, sourceToClassesMap, dirtyOutputClassesMap)

    private val cacheFormatVersion = CacheFormatVersion(targetDataRoot)

    TestOnly
    public fun dump(): String {
        return maps.map { it.dump() }.join("\n\n")
    }

    public fun markOutputClassesDirty(removedAndCompiledSources: List<File>) {
        for (sourceFile in removedAndCompiledSources) {
            val classes = sourceToClassesMap[sourceFile]
            classes.forEach { dirtyOutputClassesMap.markDirty(it.getInternalName()) }

            sourceToClassesMap.clearOutputsForSource(sourceFile)
        }
    }

    private fun getRecompilationDecision(protoChanged: Boolean, constantsChanged: Boolean, inlinesChanged: Boolean) =
            when {
                inlinesChanged -> RECOMPILE_ALL_IN_CHUNK_AND_DEPENDANTS
                constantsChanged -> RECOMPILE_OTHER_IN_CHUNK_AND_DEPENDANTS
                protoChanged -> RECOMPILE_OTHER_KOTLIN_IN_CHUNK
                else -> DO_NOTHING
            }

    public fun saveFileToCache(sourceFiles: Collection<File>, kotlinClass: LocalFileKotlinClass): RecompilationDecision {
        cacheFormatVersion.saveIfNeeded()

        val fileBytes = kotlinClass.getFileContents()
        val className = JvmClassName.byClassId(kotlinClass.getClassId())
        val header = kotlinClass.getClassHeader()

        dirtyOutputClassesMap.notDirty(className.getInternalName())
        sourceFiles.forEach { sourceToClassesMap.addSourceToClass(it, className) }

        return when {
            header.isCompatiblePackageFacadeKind() ->
                getRecompilationDecision(
                        protoChanged = protoMap.put(className, BitEncoding.decodeBytes(header.annotationData)),
                        constantsChanged = false,
                        inlinesChanged = false
                )
            header.isCompatibleClassKind() ->
                getRecompilationDecision(
                        protoChanged = protoMap.put(className, BitEncoding.decodeBytes(header.annotationData)),
                        constantsChanged = constantsMap.process(className, fileBytes),
                        inlinesChanged = inlineFunctionsMap.process(className, fileBytes)
                )
            header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }

                packagePartMap.addPackagePart(className)

                getRecompilationDecision(
                        protoChanged = false,
                        constantsChanged = constantsMap.process(className, fileBytes),
                        inlinesChanged = inlineFunctionsMap.process(className, fileBytes)
                )
            }
            else -> {
                DO_NOTHING
            }
        }
    }

    public fun clearCacheForRemovedClasses(): RecompilationDecision {
        var recompilationDecision = DO_NOTHING
        for (internalClassName in dirtyOutputClassesMap.getDirtyOutputClasses()) {
            val className = JvmClassName.byInternalName(internalClassName)

            val newDecision = getRecompilationDecision(
                    protoChanged = internalClassName in protoMap,
                    constantsChanged = internalClassName in constantsMap,
                    inlinesChanged = internalClassName in inlineFunctionsMap
            )

            recompilationDecision = recompilationDecision.merge(newDecision)

            protoMap.remove(className)
            packagePartMap.remove(className)
            constantsMap.remove(className)
            inlineFunctionsMap.remove(className)
        }
        dirtyOutputClassesMap.clear()
        return recompilationDecision
    }

    public override fun getObsoletePackageParts(): Collection<String> {
        return dirtyOutputClassesMap.getDirtyOutputClasses().filter { packagePartMap.isPackagePart(JvmClassName.byInternalName(it)) }
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

        public fun contains(key: String): Boolean = storage.containsMapping(key)

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

        TestOnly
        public fun dump(): String {
            return with(StringBuilder()) {
                with(Printer(this)) {
                    println(this@BasicMap.javaClass.getSimpleName())
                    pushIndent()

                    for (key in storage.getAllKeysWithExistingMapping().sort()) {
                        println("$key -> ${dumpValue(storage[key])}")
                    }

                    popIndent()
                }

                this
            }.toString()
        }

        protected abstract fun dumpValue(value: V): String
    }

    private inner class ProtoMap : BasicMap<ByteArray>() {
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

        public fun remove(className: JvmClassName) {
            storage.remove(className.getInternalName())
        }

        override fun dumpValue(value: ByteArray): String {
            return java.lang.Long.toHexString(value.md5())
        }
    }

    private inner class ConstantsMap : BasicMap<Map<String, Any>>() {
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

        public fun remove(className: JvmClassName) {
            put(className, null)
        }

        override fun dumpValue(value: Map<String, Any>): String {
            return StringBuilder {
                append("{")
                for (key in value.keySet().sort()) {
                    if (length() != 1) {
                        append(", ")
                    }
                    append("$key -> ${value[key]}")
                }
                append("}")
            }.toString()
        }
    }

    private object ConstantsMapExternalizer : DataExternalizer<Map<String, Any>> {
        override fun save(out: DataOutput, map: Map<String, Any>?) {
            out.writeInt(map!!.size())
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

    private inner class InlineFunctionsMap : BasicMap<Map<String, Long>>() {
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

        public fun remove(className: JvmClassName) {
            put(className, null)
        }

        override fun dumpValue(value: Map<String, Long>): String {
            return StringBuilder {
                append("{")
                for (key in value.keySet().sort()) {
                    if (length() != 1) {
                        append(", ")
                    }
                    append("$key -> ${java.lang.Long.toHexString(value[key]!!)}")
                }
                append("}")
            }.toString()
        }
    }

    private object InlineFunctionsMapExternalizer : DataExternalizer<Map<String, Long>> {
        override fun save(out: DataOutput, map: Map<String, Long>?) {
            out.writeInt(map!!.size())
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

    private inner class PackagePartMap : BasicMap<Boolean>() {
        override fun createMap(): PersistentHashMap<String, Boolean> = PersistentHashMap(
                File(baseDir, PACKAGE_PARTS),
                EnumeratorStringDescriptor(),
                BooleanDataDescriptor.INSTANCE
        )

        public fun addPackagePart(className: JvmClassName) {
            storage.put(className.getInternalName(), true)
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.getInternalName())
        }

        public fun isPackagePart(className: JvmClassName): Boolean {
            return storage.containsMapping(className.getInternalName())
        }

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class SourceToClassesMap : BasicMap<List<String>>() {
        override fun createMap(): PersistentHashMap<String, List<String>> = PersistentHashMap(
                File(baseDir, SOURCE_TO_CLASSES),
                EnumeratorStringDescriptor(),
                StringListExternalizer
        )

        public fun clearOutputsForSource(sourceFile: File) {
            storage.remove(sourceFile.getAbsolutePath())
        }

        public fun addSourceToClass(sourceFile: File, className: JvmClassName) {
            storage.appendData(sourceFile.getAbsolutePath(), { out -> IOUtil.writeUTF(out, className.getInternalName()) })
        }

        public fun get(sourceFile: File): Collection<JvmClassName> {
            return storage[sourceFile.getAbsolutePath()].orEmpty().map { JvmClassName.byInternalName(it) }
        }

        override fun dumpValue(value: List<String>) = value.toString()
    }

    private inner class DirtyOutputClassesMap : BasicMap<Boolean>() {
        override fun createMap(): PersistentHashMap<String, Boolean> = PersistentHashMap(
                File(baseDir, DIRTY_OUTPUT_CLASSES),
                EnumeratorStringDescriptor(),
                BooleanDataDescriptor.INSTANCE
        )

        public fun markDirty(className: String) {
            storage.put(className, true)
        }

        public fun notDirty(className: String) {
            storage.remove(className)
        }

        public fun getDirtyOutputClasses(): Collection<String> {
            return storage.getAllKeysWithExistingMapping()
        }

        public fun clear() {
            storage.getAllKeysWithExistingMapping().forEach { storage.remove(it) }
        }

        override fun dumpValue(value: Boolean) = ""
    }

    enum class RecompilationDecision {
        DO_NOTHING
        RECOMPILE_OTHER_KOTLIN_IN_CHUNK
        RECOMPILE_OTHER_IN_CHUNK_AND_DEPENDANTS
        RECOMPILE_ALL_IN_CHUNK_AND_DEPENDANTS

        fun merge(other: RecompilationDecision): RecompilationDecision {
            return if (other.ordinal() > this.ordinal()) other else this
        }
    }
}

private val storageProvider = object : StorageProvider<IncrementalCacheImpl>() {
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

private object ByteArrayExternalizer : DataExternalizer<ByteArray> {
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

private object StringListExternalizer : DataExternalizer<List<String>> {
    override fun save(out: DataOutput, value: List<String>) {
        value.forEach { IOUtil.writeUTF(out, it) }
    }

    override fun read(`in`: DataInput): List<String> {
        val result = ArrayList<String>()
        while ((`in` as DataInputStream).available() > 0) {
            result.add(IOUtil.readUTF(`in`))
        }
        return result
    }
}
