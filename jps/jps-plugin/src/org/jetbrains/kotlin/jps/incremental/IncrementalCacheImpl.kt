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
import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.PathStringDescriptor
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl.RecompilationDecision.DO_NOTHING
import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl.RecompilationDecision.RECOMPILE_OTHER_IN_CHUNK_AND_DEPENDANTS
import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl.RecompilationDecision.RECOMPILE_OTHER_KOTLIN_IN_CHUNK
import org.jetbrains.kotlin.jps.incremental.storage.BasicMap
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatiblePackageFacadeKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.InlineRegistering
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName.byInternalName
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.visibility
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.org.objectweb.asm.*
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File
import java.security.MessageDigest
import java.util.*

val INLINE_ANNOTATION_DESC = "Lkotlin/inline;"

private val CACHE_DIRECTORY_NAME = "kotlin"


class CacheFormatVersion(targetDataRoot: File) {
    companion object {
        // Change this when incremental cache format changes
        private val INCREMENTAL_CACHE_OWN_VERSION = 3
        private val CACHE_FORMAT_VERSION: Int = INCREMENTAL_CACHE_OWN_VERSION * 1000000 + JvmAbi.VERSION

        private val NON_INCREMENTAL_MODE_PSEUDO_VERSION = Int.MAX_VALUE

        val FORMAT_VERSION_FILE_PATH: String = "$CACHE_DIRECTORY_NAME/format-version.txt"
    }

    private val file = File(targetDataRoot, FORMAT_VERSION_FILE_PATH)

    private fun actualCacheFormatVersion() = if (IncrementalCompilation.ENABLED) CACHE_FORMAT_VERSION else NON_INCREMENTAL_MODE_PSEUDO_VERSION

    public fun isIncompatible(): Boolean {
        if (!file.exists()) return false

        val versionNumber = file.readText().toInt()
        val expectedVersionNumber = actualCacheFormatVersion()

        if (versionNumber != expectedVersionNumber) {
            KotlinBuilder.LOG.info("Incompatible incremental cache version, expected $expectedVersionNumber, actual $versionNumber")
            return true
        }
        return false
    }

    fun saveIfNeeded() {
        if (!file.exists()) {
            file.writeText(actualCacheFormatVersion().toString())
        }
    }

    fun clean() {
        file.delete()
    }
}

public class IncrementalCacheImpl(
        targetDataRoot: File,
        private val target: ModuleBuildTarget
) : StorageOwner, IncrementalCache {
    companion object {
        val PROTO_MAP = "proto.tab"
        val CONSTANTS_MAP = "constants.tab"
        val INLINE_FUNCTIONS = "inline-functions.tab"
        val PACKAGE_PARTS = "package-parts.tab"
        val SOURCE_TO_CLASSES = "source-to-classes.tab"
        val CLASS_TO_SOURCES = "class-to-sources.tab"
        val DIRTY_OUTPUT_CLASSES = "dirty-output-classes.tab"
        val DIRTY_INLINE_FUNCTIONS = "dirty-inline-functions.tab"
        val HAS_INLINE_TO = "has-inline-to.tab"

        private val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    private val baseDir = File(targetDataRoot, CACHE_DIRECTORY_NAME)

    private val String.storageFile: File
        get() = File(baseDir, this)

    private val protoMap = ProtoMap(PROTO_MAP.storageFile)
    private val constantsMap = ConstantsMap(CONSTANTS_MAP.storageFile)
    private val inlineFunctionsMap = InlineFunctionsMap(INLINE_FUNCTIONS.storageFile)
    private val packagePartMap = PackagePartMap(PACKAGE_PARTS.storageFile)
    private val sourceToClassesMap = SourceToClassesMap(SOURCE_TO_CLASSES.storageFile)
    private val classToSourcesMap = ClassToSourcesMap(CLASS_TO_SOURCES.storageFile)
    private val dirtyOutputClassesMap = DirtyOutputClassesMap(DIRTY_OUTPUT_CLASSES.storageFile)
    private val dirtyInlineFunctionsMap = DirtyInlineFunctionsMap(DIRTY_INLINE_FUNCTIONS.storageFile)
    private val hasInlineTo = InlineFunctionsFilesMap(HAS_INLINE_TO.storageFile)

    private val maps = listOf(protoMap,
                              constantsMap,
                              inlineFunctionsMap,
                              packagePartMap,
                              sourceToClassesMap,
                              dirtyOutputClassesMap,
                              hasInlineTo)

    private val cacheFormatVersion = CacheFormatVersion(targetDataRoot)
    private val dependents = arrayListOf<IncrementalCacheImpl>()
    private val outputDir = requireNotNull(target.outputDir) { "Target is expected to have output directory: $target" }

    private val inlineRegistering = object : InlineRegistering {
        override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
            hasInlineTo.add(fromPath, jvmSignature, toPath)
        }
    }

    override fun getInlineRegistering(): InlineRegistering = inlineRegistering

    public fun addDependentCache(cache: IncrementalCacheImpl) {
        dependents.add(cache)
    }

    TestOnly
    public fun dump(): String {
        return maps.map { it.dump() }.join("\n\n")
    }

    public fun markOutputClassesDirty(removedAndCompiledSources: List<File>) {
        for (sourceFile in removedAndCompiledSources) {
            val classes = sourceToClassesMap[sourceFile]
            classes.forEach {
                dirtyOutputClassesMap.markDirty(it.internalName)
                classToSourcesMap.remove(it)
            }

            sourceToClassesMap.clearOutputsForSource(sourceFile)
        }
    }

    public fun getFilesToReinline(): Collection<File> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)

        for ((className, functions) in dirtyInlineFunctionsMap.getEntries()) {
            val sourceFiles = classToSourcesMap[className]

            for (sourceFile in sourceFiles) {
                val targetFiles = functions.flatMap { hasInlineTo[sourceFile, it] }
                result.addAll(targetFiles)
            }

            var internalName = className.internalName

            if (packagePartMap.isPackagePart(className)) {
                val packageInternalName = PackageClassUtils.getPackageClassInternalName(className.packageFqName)
                val packageJvmName = JvmClassName.byInternalName(packageInternalName)
                internalName = packageJvmName.internalName
            }

            val classFilePath = getClassFilePath(internalName)

            for (dependent in dependents) {
                val targetFiles = functions.flatMap { dependent.hasInlineTo[classFilePath, it] }
                result.addAll(targetFiles)
            }
        }

        dirtyInlineFunctionsMap.clean()
        return result.map { File(it) }
    }

    override fun getClassFilePath(internalClassName: String): String {
        return File(outputDir, "$internalClassName.class").canonicalPath
    }

    private fun getRecompilationDecision(protoChanged: Boolean, constantsChanged: Boolean) =
            when {
                constantsChanged -> RECOMPILE_OTHER_IN_CHUNK_AND_DEPENDANTS
                protoChanged -> RECOMPILE_OTHER_KOTLIN_IN_CHUNK
                else -> DO_NOTHING
            }

    public fun saveCacheFormatVersion() {
        cacheFormatVersion.saveIfNeeded()
    }

    public fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File): RecompilationDecision {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.put(jvmClassName, file.readBytes(), isPackage = false, checkChangesIsOpenPart = false)
        dirtyOutputClassesMap.notDirty(MODULE_MAPPING_FILE_NAME)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
        return DO_NOTHING
    }

    public fun saveFileToCache(sourceFiles: Collection<File>, kotlinClass: LocalFileKotlinClass): RecompilationDecision {
        val fileBytes = kotlinClass.getFileContents()
        val className = JvmClassName.byClassId(kotlinClass.getClassId())
        val header = kotlinClass.getClassHeader()

        dirtyOutputClassesMap.notDirty(className.getInternalName())
        sourceFiles.forEach {
            sourceToClassesMap.add(it, className)
            classToSourcesMap.add(className, it)
        }

        inlineFunctionsMap.process(className, fileBytes)

        val decision = when {
            header.isCompatiblePackageFacadeKind() ->
                getRecompilationDecision(
                        protoChanged = protoMap.put(className, BitEncoding.decodeBytes(header.annotationData!!), isPackage = true),
                        constantsChanged = false
                )
            header.isCompatibleFileFacadeKind() -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)
                getRecompilationDecision(
                        protoChanged = protoMap.put(className, BitEncoding.decodeBytes(header.annotationData!!), isPackage = true),
                        constantsChanged = constantsMap.process(className, fileBytes)
                )
            }
            header.isCompatibleClassKind() ->
                when (header.classKind!!) {
                    JvmAnnotationNames.KotlinClass.Kind.CLASS -> getRecompilationDecision(
                            protoChanged = protoMap.put(className, BitEncoding.decodeBytes(header.annotationData!!), isPackage = false),
                            constantsChanged = constantsMap.process(className, fileBytes)
                    )

                    JvmAnnotationNames.KotlinClass.Kind.LOCAL_CLASS, JvmAnnotationNames.KotlinClass.Kind.ANONYMOUS_OBJECT -> DO_NOTHING
                }
            header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }

                packagePartMap.addPackagePart(className)

                getRecompilationDecision(
                        protoChanged = false,
                        constantsChanged = constantsMap.process(className, fileBytes)
                )
            }
            else -> {
                DO_NOTHING
            }
        }
        if (decision != DO_NOTHING) {
            KotlinBuilder.LOG.debug("$decision because $className is changed")
        }
        return decision
    }

    public fun clearCacheForRemovedClasses(): RecompilationDecision {
        var recompilationDecision = DO_NOTHING
        for (internalClassName in dirtyOutputClassesMap.getDirtyOutputClasses()) {
            val className = JvmClassName.byInternalName(internalClassName)

            val newDecision = getRecompilationDecision(
                    protoChanged = internalClassName in protoMap,
                    constantsChanged = internalClassName in constantsMap
            )
            if (newDecision != DO_NOTHING) {
                KotlinBuilder.LOG.debug("$newDecision because $internalClassName is removed")
            }

            recompilationDecision = recompilationDecision.merge(newDecision)

            protoMap.remove(className)
            packagePartMap.remove(className)
            constantsMap.remove(className)
            inlineFunctionsMap.remove(className)
        }
        dirtyOutputClassesMap.clean()
        return recompilationDecision
    }

    override fun getObsoletePackageParts(): Collection<String> {
        val obsoletePackageParts =
                dirtyOutputClassesMap.getDirtyOutputClasses().filter { packagePartMap.isPackagePart(JvmClassName.byInternalName(it)) }
        KotlinBuilder.LOG.debug("Obsolete package parts: ${obsoletePackageParts}")
        return obsoletePackageParts
    }

    override fun getPackagePartData(fqName: String): ByteArray? {
        return protoMap[JvmClassName.byInternalName(fqName)]
    }

    override fun getModuleMappingData(): ByteArray? {
        return protoMap[JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)]
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

    private inner class ProtoMap(storageFile: File) : BasicMap<ByteArray>(storageFile, ByteArrayExternalizer) {

        public fun put(className: JvmClassName, data: ByteArray, isPackage: Boolean, checkChangesIsOpenPart: Boolean = true): Boolean {
            val key = className.getInternalName()
            val oldData = storage[key]
            if (Arrays.equals(data, oldData)) {
                return false
            }
            storage.put(key, data)

            if (oldData != null && checkChangesIsOpenPart && isOpenPartNotChanged(oldData, data, isPackage)) {
                return false
            }

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

        private fun isOpenPartNotChanged(oldData: ByteArray, newData: ByteArray, isPackageFacade: Boolean): Boolean {
            if (isPackageFacade) {
                return isPackageFacadeOpenPartNotChanged(oldData, newData)
            }
            else {
                return isClassOpenPartNotChanged(oldData, newData)
            }
        }

        private fun isPackageFacadeOpenPartNotChanged(oldData: ByteArray, newData: ByteArray): Boolean {
            val oldPackageData = JvmProtoBufUtil.readPackageDataFrom(oldData)
            val newPackageData = JvmProtoBufUtil.readPackageDataFrom(newData)

            val compareObject = ProtoCompareGenerated(oldPackageData.nameResolver, newPackageData.nameResolver)
            return compareObject.checkEquals(oldPackageData.packageProto, newPackageData.packageProto)
        }

        private fun isClassOpenPartNotChanged(oldData: ByteArray, newData: ByteArray): Boolean {
            val oldClassData = JvmProtoBufUtil.readClassDataFrom(oldData)
            val newClassData = JvmProtoBufUtil.readClassDataFrom(newData)

            val compareObject = object : ProtoCompareGenerated(oldClassData.nameResolver, newClassData.nameResolver) {
                override fun checkEqualsClassMember(old: ProtoBuf.Class, new: ProtoBuf.Class): Boolean =
                        checkEquals(old.memberList, new.memberList)

                override fun checkEqualsClassSecondaryConstructor(old: ProtoBuf.Class, new: ProtoBuf.Class): Boolean =
                        checkEquals(old.secondaryConstructorList, new.secondaryConstructorList)

                private fun checkEquals(oldList: List<ProtoBuf.Callable>, newList: List<ProtoBuf.Callable>): Boolean {
                    val oldListFiltered = oldList.filter { !it.isPrivate() }
                    val newListFiltered = newList.filter { !it.isPrivate() }

                    if (oldListFiltered.size() != newListFiltered.size()) return false

                    for (i in oldListFiltered.indices) {
                        if (!checkEquals(oldListFiltered[i], newListFiltered[i])) return false
                    }

                    return true
                }

                private fun ProtoBuf.Callable.isPrivate(): Boolean = Visibilities.isPrivate(visibility(Flags.VISIBILITY.get(flags)))
            }

            return compareObject.checkEquals(oldClassData.classProto, newClassData.classProto)
        }
    }

    private inner class ConstantsMap(storageFile: File) : BasicMap<Map<String, Any>>(storageFile, ConstantsMapExternalizer) {
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

        override fun dumpValue(value: Map<String, Any>): String =
                value.dumpMap(Any::toString)
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

    private inner class InlineFunctionsMap(storageFile: File) : BasicMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
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

        public fun process(className: JvmClassName, bytes: ByteArray): Boolean {
            return put(className, getInlineFunctionsMap(bytes))
        }

        private fun put(className: JvmClassName, newMap: Map<String, Long>): Boolean {
            val internalName = className.internalName
            val oldMap = storage[internalName] ?: emptyMap()

            val changed = hashSetOf<String>()
            val allFunctions = oldMap.keySet() + newMap.keySet()

            for (fn in allFunctions) {
                val oldHash = oldMap[fn]
                val newHash = newMap[fn]

                if (oldHash != newHash) {
                    changed.add(fn)
                }
            }

            when {
                newMap.isNotEmpty() -> storage.put(internalName, newMap)
                else -> storage.remove(internalName)
            }

            if (changed.isNotEmpty()) {
                dirtyInlineFunctionsMap.put(className, changed.toList())
                return true
            }

            return false
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
                value.dumpMap { java.lang.Long.toHexString(it) }
    }

    private inner class PackagePartMap(storageFile: File) : BasicMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
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

    private inner class SourceToClassesMap(storageFile: File) : BasicMap<List<String>>(storageFile, StringListExternalizer) {
        override val keyDescriptor: KeyDescriptor<String>
            get() = PathStringDescriptor.INSTANCE

        public fun clearOutputsForSource(sourceFile: File) {
            storage.remove(sourceFile.getAbsolutePath())
        }

        public fun add(sourceFile: File, className: JvmClassName) {
            storage.appendData(sourceFile.getAbsolutePath(), { out -> IOUtil.writeUTF(out, className.getInternalName()) })
        }

        public fun get(sourceFile: File): Collection<JvmClassName> {
            return storage[sourceFile.getAbsolutePath()].orEmpty().map { JvmClassName.byInternalName(it) }
        }

        override fun dumpValue(value: List<String>) = value.toString()
    }

    private inner class ClassToSourcesMap(storageFile: File) : BasicMap<Collection<String>>(storageFile, PathCollectionExternalizer) {
        public fun get(className: JvmClassName): Collection<String> =
                storage[className.internalName] ?: emptySet()

        public fun add(className: JvmClassName, sourceFile: File) {
            storage.appendData(className.internalName) { out ->
                IOUtil.writeUTF(out, sourceFile.normalizedPath)
            }
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Collection<String>): String =
                value.dumpCollection()
    }

    private inner class DirtyOutputClassesMap(storageFile: File) : BasicMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
        public fun markDirty(className: String) {
            storage.put(className, true)
        }

        public fun notDirty(className: String) {
            storage.remove(className)
        }

        public fun getDirtyOutputClasses(): Collection<String> {
            return storage.getAllKeysWithExistingMapping()
        }

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class DirtyInlineFunctionsMap(storageFile: File) : BasicMap<List<String>>(storageFile, StringListExternalizer) {
        public fun getEntries(): Map<JvmClassName, List<String>> =
            storage.allKeysWithExistingMapping
                   .toMap(JvmClassName::byInternalName) { storage[it] }

        public fun put(className: JvmClassName, changedFunctions: List<String>) {
            storage.put(className.internalName, changedFunctions)
        }

        override fun dumpValue(value: List<String>) =
                value.dumpCollection()
    }

    /**
     * Mapping: sourceFile->{inlineFunction->...targetFiles}
     *
     * Where:
     *  * sourceFile - path to some kotlin source
     *  * inlineFunction - jvmSignature of some inline function in source file
     *  * target files - collection of files inlineFunction has been inlined to
     */
    private inner class InlineFunctionsFilesMap(storageFile: File) : BasicMap<Map<String, Collection<String>>>(storageFile, StringToPathsMapExternalizer) {
        override val keyDescriptor: KeyDescriptor<String>
            get() = PathStringDescriptor()

        private val cache = THashMap<String, MutableMap<String, MutableCollection<String>>>(FileUtil.PATH_HASHING_STRATEGY)

        public fun add(sourcePath: String, jvmSignature: String, targetPath: String) {
            val mapping = getMappingFromCache(sourcePath.normalizedPath)
            val paths = mapping.getOrPut(jvmSignature) { THashSet(FileUtil.PATH_HASHING_STRATEGY) }
            paths.add(targetPath.normalizedPath)
        }

        public fun get(sourceFile: String, jvmSignature: String): Collection<String> {
            val normalizedPath = sourceFile.normalizedPath
            if (normalizedPath !in cache && !storage.containsMapping(normalizedPath)) return emptySet()

            val mapping = getMappingFromCache(normalizedPath)
            return mapping[jvmSignature] ?: emptySet()
        }

        override fun clean() {
            cache.clear()
            super.clean()
        }

        override fun flush(memoryCachesOnly: Boolean) {
            for ((k, v) in cache) {
                storage.put(k, v)
            }

            super.flush(memoryCachesOnly)
        }

        override fun dumpValue(value: Map<String, Collection<String>>) =
                value.dumpMap { it.dumpCollection() }

        private fun getMappingFromCache(sourcePath: String): MutableMap<String, MutableCollection<String>> {
            val cachedValue = cache[sourcePath]
            if (cachedValue != null) return cachedValue

            val mapping = storage[sourcePath] ?: emptyMap()
            val mutableMapping = hashMapOf<String, MutableCollection<String>>()

            for ((k, v) in mapping) {
                val paths = THashSet(v, FileUtil.PATH_HASHING_STRATEGY)
                mutableMapping[k] = paths
            }

            cache[sourcePath] = mutableMapping
            return mutableMapping
        }
    }

    enum class RecompilationDecision {
        DO_NOTHING,
        RECOMPILE_OTHER_KOTLIN_IN_CHUNK,
        RECOMPILE_OTHER_IN_CHUNK_AND_DEPENDANTS,
        RECOMPILE_ALL_IN_CHUNK_AND_DEPENDANTS;

        fun merge(other: RecompilationDecision): RecompilationDecision {
            return if (other.ordinal() > this.ordinal()) other else this
        }
    }
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

private object StringToPathsMapExternalizer : StringMapExternalizer<Collection<String>>() {
    override fun readValue(input: DataInput): Collection<String> {
        val size = input.readInt()
        val paths = THashSet(size, FileUtil.PATH_HASHING_STRATEGY)

        repeat(size) {
            paths.add(IOUtil.readUTF(input))
        }

        return paths
    }

    override fun writeValue(output: DataOutput, value: Collection<String>) {
        output.writeInt(value.size())

        for (path in value) {
            IOUtil.writeUTF(output, path)
        }
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

private val File.normalizedPath: String
    get() = FileUtil.toSystemIndependentName(canonicalPath)

private val String.normalizedPath: String
    get() = FileUtil.toSystemIndependentName(this)

TestOnly
private fun <K : Comparable<K>, V> Map<K, V>.dumpMap(dumpValue: (V)->String): String =
        StringBuilder {
            append("{")
            for (key in keySet().sort()) {
                if (length() != 1) {
                    append(", ")
                }

                val value = get(key)?.let(dumpValue) ?: "null"
                append("$key -> $value")
            }
            append("}")
        }.toString()

TestOnly
public fun <T : Comparable<T>> Collection<T>.dumpCollection(): String =
        "[${sort().map(Any::toString).join(", ")}]"
