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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.PathStringDescriptor
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.jps.build.GeneratedJvmClass
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.incremental.storage.BasicMap
import org.jetbrains.kotlin.jps.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName.byInternalName
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.org.objectweb.asm.*
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File
import java.security.MessageDigest
import java.util.*

val INLINE_ANNOTATION_DESC = "Lkotlin/inline;"

internal val CACHE_DIRECTORY_NAME = "kotlin"

@TestOnly
public fun getCacheDirectoryName(): String =
        CACHE_DIRECTORY_NAME

public class IncrementalCacheImpl(
        targetDataRoot: File,
        private val target: ModuleBuildTarget
) : StorageOwner, IncrementalCache {
    companion object {
        val CACHE_EXTENSION = "tab"

        private val PROTO_MAP = "proto"
        private val CONSTANTS_MAP = "constants"
        private val INLINE_FUNCTIONS = "inline-functions"
        private val PACKAGE_PARTS = "package-parts"
        private val SOURCE_TO_CLASSES = "source-to-classes"
        private val DIRTY_OUTPUT_CLASSES = "dirty-output-classes"
        private val DIRTY_INLINE_FUNCTIONS = "dirty-inline-functions"
        private val INLINED_TO = "inlined-to"

        private val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    private val baseDir = File(targetDataRoot, CACHE_DIRECTORY_NAME)
    private val maps = arrayListOf<BasicMap<*, *>>()

    private val String.storageFile: File
        get() = File(baseDir, this + "." + CACHE_EXTENSION)

    private fun <K, V, M : BasicMap<K, V>> registerMap(map: M): M {
        maps.add(map)
        return map
    }

    private val protoMap = registerMap(ProtoMap(PROTO_MAP.storageFile))
    private val constantsMap = registerMap(ConstantsMap(CONSTANTS_MAP.storageFile))
    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))
    private val packagePartMap = registerMap(PackagePartMap(PACKAGE_PARTS.storageFile))
    private val sourceToClassesMap = registerMap(SourceToClassesMap(SOURCE_TO_CLASSES.storageFile))
    private val dirtyOutputClassesMap = registerMap(DirtyOutputClassesMap(DIRTY_OUTPUT_CLASSES.storageFile))
    // TODO: can be removed?
    private val dirtyInlineFunctionsMap = registerMap(DirtyInlineFunctionsMap(DIRTY_INLINE_FUNCTIONS.storageFile))
    private val inlinedTo = registerMap(InlineFunctionsFilesMap(INLINED_TO.storageFile))

    private val cacheFormatVersion = CacheFormatVersion(targetDataRoot)
    private val dependents = arrayListOf<IncrementalCacheImpl>()
    private val outputDir = requireNotNull(target.outputDir) { "Target is expected to have output directory: $target" }

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        inlinedTo.add(fromPath, jvmSignature, toPath)
    }

    public fun addDependentCache(cache: IncrementalCacheImpl) {
        dependents.add(cache)
    }

    @TestOnly
    public fun dump(): String {
        return maps.map { it.dump() }.join("\n\n")
    }

    public fun markOutputClassesDirty(removedAndCompiledSources: List<File>) {
        for (sourceFile in removedAndCompiledSources) {
            val classes = sourceToClassesMap[sourceFile]
            classes.forEach {
                dirtyOutputClassesMap.markDirty(it.internalName)
            }

            sourceToClassesMap.clearOutputsForSource(sourceFile)
        }
    }

    public fun getFilesToReinline(): Collection<File> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)

        for ((className, functions) in dirtyInlineFunctionsMap.getEntries()) {
            val internalName =
                if (packagePartMap.isPackagePart(className)) {
                    val packageInternalName = PackageClassUtils.getPackageClassInternalName(className.packageFqName)
                    val packageJvmName = JvmClassName.byInternalName(packageInternalName)
                    packageJvmName.internalName
                }
                else {
                    className.internalName
                }

            val classFilePath = getClassFilePath(internalName)

            fun addFilesAffectedByChangedInlineFuns(cache: IncrementalCacheImpl) {
                val targetFiles = functions.flatMap { cache.inlinedTo[classFilePath, it] }
                result.addAll(targetFiles)
            }

            addFilesAffectedByChangedInlineFuns(this)
            dependents.forEach(::addFilesAffectedByChangedInlineFuns)
        }

        dirtyInlineFunctionsMap.clean()
        return result.map { File(it) }
    }

    override fun getClassFilePath(internalClassName: String): String {
        return File(outputDir, "$internalClassName.class").canonicalPath
    }

    public fun saveCacheFormatVersion() {
        cacheFormatVersion.saveIfNeeded()
    }

    public fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File): ChangesInfo {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.process(jvmClassName, file.readBytes(), emptyArray<String>(), isPackage = false, checkChangesIsOpenPart = false)
        dirtyOutputClassesMap.notDirty(MODULE_MAPPING_FILE_NAME)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
        return ChangesInfo.NO_CHANGES
    }

    public fun saveFileToCache(generatedClass: GeneratedJvmClass): ChangesInfo {
        val sourceFiles: Collection<File> = generatedClass.sourceFiles
        val kotlinClass: LocalFileKotlinClass = generatedClass.outputClass
        val className = JvmClassName.byClassId(kotlinClass.classId)

        dirtyOutputClassesMap.notDirty(className.internalName)
        sourceFiles.forEach {
            sourceToClassesMap.add(it, className)
        }

        val header = kotlinClass.classHeader
        val changesInfo = when {
            header.isCompatiblePackageFacadeKind() ->
                protoMap.process(kotlinClass, isPackage = true)
            header.isCompatibleFileFacadeKind() -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)

                protoMap.process(kotlinClass, isPackage = true) +
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass)
            }
            header.isCompatibleClassKind() && JvmAnnotationNames.KotlinClass.Kind.CLASS == header.classKind ->
                protoMap.process(kotlinClass, isPackage = false) +
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass)
            header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)

                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass)
            }
            else -> ChangesInfo.NO_CHANGES
        }

        changesInfo.logIfSomethingChanged(className)
        return changesInfo
    }

    private fun ChangesInfo.logIfSomethingChanged(className: JvmClassName) {
        if (this == ChangesInfo.NO_CHANGES) return

        KotlinBuilder.LOG.debug("$className is changed: $this")
    }

    public fun clearCacheForRemovedClasses(): ChangesInfo {
        val dirtyClasses = dirtyOutputClassesMap
                                .getDirtyOutputClasses()
                                .map(JvmClassName::byInternalName)
                                .toList()

        val changesInfo = dirtyClasses.fold(ChangesInfo.NO_CHANGES) { info, className ->
            val newInfo = ChangesInfo(protoChanged = className in protoMap,
                                      constantsChanged = className in constantsMap)
            newInfo.logIfSomethingChanged(className)
            info + newInfo
        }

        dirtyClasses.forEach {
            protoMap.remove(it)
            packagePartMap.remove(it)
            constantsMap.remove(it)
            inlineFunctionsMap.remove(it)
        }
        dirtyOutputClassesMap.clean()
        return changesInfo
    }

    override fun getObsoletePackageParts(): Collection<String> {
        val obsoletePackageParts =
                dirtyOutputClassesMap.getDirtyOutputClasses().filter { packagePartMap.isPackagePart(JvmClassName.byInternalName(it)) }
        KotlinBuilder.LOG.debug("Obsolete package parts: ${obsoletePackageParts}")
        return obsoletePackageParts
    }

    override fun getPackagePartData(fqName: String): JvmPackagePartProto? {
        return protoMap[JvmClassName.byInternalName(fqName)]?.let { value ->
            JvmPackagePartProto(value.bytes, value.strings)
        }
    }

    override fun getModuleMappingData(): ByteArray? {
        return protoMap[JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)]?.bytes
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

    private inner class ProtoMap(storageFile: File) : BasicStringMap<ProtoMapValue>(storageFile, ProtoMapValueExternalizer) {

        public fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean, checkChangesIsOpenPart: Boolean = true): ChangesInfo {
            val header = kotlinClass.classHeader
            val bytes = BitEncoding.decodeBytes(header.annotationData!!)
            return put(kotlinClass.className, bytes, header.strings!!, isPackage, checkChangesIsOpenPart)
        }

        public fun process(className: JvmClassName, data: ByteArray, strings: Array<String>, isPackage: Boolean, checkChangesIsOpenPart: Boolean): ChangesInfo {
            return put(className, data, strings, isPackage, checkChangesIsOpenPart)
        }

        private fun put(
                className: JvmClassName, bytes: ByteArray, strings: Array<String>, isPackage: Boolean, checkChangesIsOpenPart: Boolean
        ): ChangesInfo {
            val key = className.internalName
            val oldData = storage[key]
            val data = ProtoMapValue(isPackage, bytes, strings)

            if (oldData == null ||
                !Arrays.equals(bytes, oldData.bytes) ||
                !Arrays.equals(strings, oldData.strings) ||
                isPackage != oldData.isPackageFacade) {
                storage[key] = data
            }

            return ChangesInfo(protoChanged = oldData == null ||
                                              !checkChangesIsOpenPart ||
                                              difference(oldData, data) != DifferenceKind.NONE)
        }

        public fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        public fun get(className: JvmClassName): ProtoMapValue? =
                storage[className.internalName]

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: ProtoMapValue): String {
            return (if (value.isPackageFacade) "1" else "0") + java.lang.Long.toHexString(value.bytes.md5())
        }
    }

    private inner class ConstantsMap(storageFile: File) : BasicStringMap<Map<String, Any>>(storageFile, ConstantsMapExternalizer) {
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

        fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        public fun process(kotlinClass: LocalFileKotlinClass): ChangesInfo {
            return put(kotlinClass.className, getConstantsMap(kotlinClass.fileContents))
        }

        private fun put(className: JvmClassName, constantsMap: Map<String, Any>?): ChangesInfo {
            val key = className.getInternalName()

            val oldMap = storage[key]
            if (oldMap == constantsMap) return ChangesInfo.NO_CHANGES

            if (constantsMap != null) {
                storage[key] = constantsMap
            }
            else {
                storage.remove(key)
            }

            return ChangesInfo(constantsChanged = true)
        }

        public fun remove(className: JvmClassName) {
            put(className, null)
        }

        override fun dumpValue(value: Map<String, Any>): String =
                value.dumpMap(Any::toString)
    }

    private object ConstantsMapExternalizer : DataExternalizer<Map<String, Any>> {
        override fun save(out: DataOutput, map: Map<String, Any>?) {
            out.writeInt(map!!.size())
            for (name in map.keySet().sorted()) {
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

            repeat(size) {
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
            INT, FLOAT, LONG, DOUBLE, STRING
        }
    }

    private inner class InlineFunctionsMap(storageFile: File) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
        private fun getInlineFunctionsMap(bytes: ByteArray): Map<String, Long> {
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

            return result
        }

        public fun process(kotlinClass: LocalFileKotlinClass): ChangesInfo {
            return put(kotlinClass.className, getInlineFunctionsMap(kotlinClass.fileContents))
        }

        private fun put(className: JvmClassName, newMap: Map<String, Long>): ChangesInfo {
            val internalName = className.internalName
            val oldMap = storage[internalName] ?: emptyMap()

            val added = hashSetOf<String>()
            val changed = hashSetOf<String>()
            val allFunctions = oldMap.keySet() + newMap.keySet()

            for (fn in allFunctions) {
                val oldHash = oldMap[fn]
                val newHash = newMap[fn]

                when {
                    oldHash == null -> added.add(fn)
                    oldHash != newHash -> changed.add(fn)
                }
            }

            when {
                newMap.isNotEmpty() -> storage[internalName] = newMap
                else -> storage.remove(internalName)
            }

            if (changed.isNotEmpty()) {
                dirtyInlineFunctionsMap.put(className, changed.toList())
            }

            return ChangesInfo(inlineChanged = changed.isNotEmpty(),
                               inlineAdded = added.isNotEmpty())
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
                value.dumpMap { java.lang.Long.toHexString(it) }
    }

    private inner class PackagePartMap(storageFile: File) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
        public fun addPackagePart(className: JvmClassName) {
            storage[className.internalName] = true
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        public fun isPackagePart(className: JvmClassName): Boolean =
                className.internalName in storage

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class SourceToClassesMap(storageFile: File) : BasicStringMap<List<String>>(storageFile, PathStringDescriptor.INSTANCE, StringListExternalizer) {
        public fun clearOutputsForSource(sourceFile: File) {
            storage.remove(sourceFile.absolutePath)
        }

        public fun add(sourceFile: File, className: JvmClassName) {
            storage.append(sourceFile.absolutePath, { out -> IOUtil.writeUTF(out, className.getInternalName()) })
        }

        public fun get(sourceFile: File): Collection<JvmClassName> =
                storage[sourceFile.absolutePath].orEmpty().map { JvmClassName.byInternalName(it) }

        override fun dumpValue(value: List<String>) = value.toString()
    }

    private inner class DirtyOutputClassesMap(storageFile: File) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
        public fun markDirty(className: String) {
            storage[className] = true
        }

        public fun notDirty(className: String) {
            storage.remove(className)
        }

        public fun getDirtyOutputClasses(): Collection<String> =
                storage.keys

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class DirtyInlineFunctionsMap(storageFile: File) : BasicStringMap<List<String>>(storageFile, StringListExternalizer) {
        public fun getEntries(): Map<JvmClassName, List<String>> =
            storage.keys.toMap(JvmClassName::byInternalName) { storage[it]!! }

        public fun put(className: JvmClassName, changedFunctions: List<String>) {
            storage[className.internalName] = changedFunctions
        }

        override fun dumpValue(value: List<String>) =
                value.dumpCollection()
    }


    /**
     * Mapping: (sourceFile+inlineFunction)->(targetFiles)
     *
     * Where:
     *  * sourceFile - path to some kotlin source
     *  * inlineFunction - jvmSignature of some inline function in source file
     *  * target files - collection of files inlineFunction has been inlined to
     */
    private inner class InlineFunctionsFilesMap(storageFile: File) : BasicMap<PathFunctionPair, Collection<String>>(storageFile, PathFunctionPairKeyDescriptor, PathCollectionExternalizer) {
        public fun add(sourcePath: String, jvmSignature: String, targetPath: String) {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            storage.append(key) { out ->
                IOUtil.writeUTF(out, targetPath)
            }
        }

        public fun get(sourcePath: String, jvmSignature: String): Collection<String> {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            return storage[key] ?: emptySet()
        }

        override fun dumpKey(key: PathFunctionPair): String =
            "(${key.path}, ${key.function})"

        override fun dumpValue(value: Collection<String>) =
            value.dumpCollection()
    }
}

data class ChangesInfo(
        public val protoChanged: Boolean = false,
        public val constantsChanged: Boolean = false,
        public val inlineChanged: Boolean = false,
        public val inlineAdded: Boolean = false
) {
    companion object {
        public val NO_CHANGES: ChangesInfo = ChangesInfo()
    }

    public fun plus(other: ChangesInfo): ChangesInfo =
            ChangesInfo(protoChanged || other.protoChanged,
                        constantsChanged || other.constantsChanged,
                        inlineChanged || other.inlineChanged,
                        inlineAdded || other.inlineAdded)
}


public fun BuildDataPaths.getKotlinCacheVersion(target: BuildTarget<*>): CacheFormatVersion = CacheFormatVersion(getTargetDataRoot(target))

private data class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget
) : StorageProvider<IncrementalCacheImpl>() {
    override fun createStorage(targetDataDir: File): IncrementalCacheImpl =
            IncrementalCacheImpl(targetDataDir, target)
}

public fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): IncrementalCacheImpl =
        getStorage(target, KotlinIncrementalStorageProvider(target))

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

private abstract class StringMapExternalizer<T> : DataExternalizer<Map<String, T>> {
    override fun save(out: DataOutput, map: Map<String, T>?) {
        out.writeInt(map!!.size())

        for ((key, value) in map.entrySet()) {
            IOUtil.writeString(key, out)
            writeValue(out, value)
        }
    }

    override fun read(`in`: DataInput): Map<String, T>? {
        val size = `in`.readInt()
        val map = HashMap<String, T>(size)

        repeat(size) {
            val name = IOUtil.readString(`in`)!!
            map[name] = readValue(`in`)
        }

        return map
    }

    protected abstract fun writeValue(output: DataOutput, value: T)
    protected abstract fun readValue(input: DataInput): T
}

private object StringToLongMapExternalizer : StringMapExternalizer<Long>() {
    override fun readValue(input: DataInput): Long =
            input.readLong()

    override fun writeValue(output: DataOutput, value: Long) {
        output.writeLong(value)
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

private object PathCollectionExternalizer : DataExternalizer<Collection<String>> {
    override fun save(out: DataOutput, value: Collection<String>) {
        for (str in value) {
            IOUtil.writeUTF(out, str)
        }
    }

    override fun read(`in`: DataInput): Collection<String> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)
        val stream = `in` as DataInputStream
        while (stream.available() > 0) {
            val str = IOUtil.readUTF(stream)
            result.add(str)
        }
        return result
    }
}

@TestOnly
private fun <K : Comparable<K>, V> Map<K, V>.dumpMap(dumpValue: (V)->String): String =
        StringBuilder {
            append("{")
            for (key in keySet().sorted()) {
                if (length() != 1) {
                    append(", ")
                }

                val value = get(key)?.let(dumpValue) ?: "null"
                append("$key -> $value")
            }
            append("}")
        }.toString()

@TestOnly
public fun <T : Comparable<T>> Collection<T>.dumpCollection(): String =
        "[${sorted().map(Any::toString).join(", ")}]"

private class PathFunctionPair(
        public val path: String,
        public val function: String
): Comparable<PathFunctionPair> {
    override fun compareTo(other: PathFunctionPair): Int {
        val pathComp = FileUtil.comparePaths(path, other.path)

        if (pathComp != 0) return pathComp

        return function.compareTo(other.function)
    }

    override fun equals(other: Any?): Boolean =
        when (other) {
            is PathFunctionPair ->
                FileUtil.pathsEqual(path, other.path) && function == other.function
            else ->
                false
        }

    override fun hashCode(): Int = 31 * FileUtil.pathHashCode(path) + function.hashCode()
}

private object PathFunctionPairKeyDescriptor : KeyDescriptor<PathFunctionPair> {
    override fun getHashCode(value: PathFunctionPair): Int =
            value.hashCode()

    override fun isEqual(val1: PathFunctionPair, val2: PathFunctionPair): Boolean =
            val1 == val2

    override fun read(`in`: DataInput): PathFunctionPair {
        val path = IOUtil.readUTF(`in`)
        val function = IOUtil.readUTF(`in`)
        return PathFunctionPair(path, function)
    }

    override fun save(out: DataOutput, value: PathFunctionPair) {
        IOUtil.writeUTF(out, value.path)
        IOUtil.writeUTF(out, value.function)
    }

}

private object ProtoMapValueExternalizer : DataExternalizer<ProtoMapValue> {
    override fun save(out: DataOutput, value: ProtoMapValue) {
        out.writeBoolean(value.isPackageFacade)
        out.writeInt(value.bytes.size())
        out.write(value.bytes)
        out.writeInt(value.strings.size())
        for (string in value.strings) {
            out.writeUTF(string)
        }
    }

    override fun read(`in`: DataInput): ProtoMapValue {
        val isPackageFacade = `in`.readBoolean()
        val bytesLength = `in`.readInt()
        val bytes = ByteArray(bytesLength)
        `in`.readFully(bytes, 0, bytesLength)
        val stringsLength = `in`.readInt()
        val strings = Array<String>(stringsLength) { `in`.readUTF() }
        return ProtoMapValue(isPackageFacade, bytes, strings)
    }
}
