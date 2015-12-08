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

import com.google.protobuf.MessageLite
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IOUtil
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.inline.inlineFunctionsJvmNames
import org.jetbrains.kotlin.jps.build.GeneratedJvmClass
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleMultifileClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleMultifileClassPartKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.security.MessageDigest
import java.util.*

val KOTLIN_CACHE_DIRECTORY_NAME = "kotlin"

public class IncrementalCacheImpl(
        private val target: ModuleBuildTarget,
        paths: BuildDataPaths
) : BasicMapsOwner(), IncrementalCache {
    companion object {
        val PROTO_MAP = "proto"
        val CONSTANTS_MAP = "constants"
        val INLINE_FUNCTIONS = "inline-functions"
        val PACKAGE_PARTS = "package-parts"
        val MULTIFILE_CLASS_FACADES = "multifile-class-facades"
        val MULTIFILE_CLASS_PARTS = "multifile-class-parts"
        val SOURCE_TO_CLASSES = "source-to-classes"
        val DIRTY_OUTPUT_CLASSES = "dirty-output-classes"
        val DIRTY_INLINE_FUNCTIONS = "dirty-inline-functions"
        val INLINED_TO = "inlined-to"

        private val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    private val baseDir = File(paths.getTargetDataRoot(target), KOTLIN_CACHE_DIRECTORY_NAME)
    private val cacheVersionProvider = CacheVersionProvider(paths)

    private val String.storageFile: File
        get() = File(baseDir, this + "." + CACHE_EXTENSION)

    private val protoMap = registerMap(ProtoMap(PROTO_MAP.storageFile))
    private val constantsMap = registerMap(ConstantsMap(CONSTANTS_MAP.storageFile))
    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))
    private val packagePartMap = registerMap(PackagePartMap(PACKAGE_PARTS.storageFile))
    private val multifileClassFacadeMap = registerMap(MultifileClassFacadeMap(MULTIFILE_CLASS_FACADES.storageFile))
    private val multifileClassPartMap = registerMap(MultifileClassPartMap(MULTIFILE_CLASS_PARTS.storageFile))
    private val sourceToClassesMap = registerMap(SourceToClassesMap(SOURCE_TO_CLASSES.storageFile))
    private val dirtyOutputClassesMap = registerMap(DirtyOutputClassesMap(DIRTY_OUTPUT_CLASSES.storageFile))
    private val dirtyInlineFunctionsMap = registerMap(DirtyInlineFunctionsMap(DIRTY_INLINE_FUNCTIONS.storageFile))
    private val inlinedTo = registerMap(InlineFunctionsFilesMap(INLINED_TO.storageFile))

    private val dependents = arrayListOf<IncrementalCacheImpl>()
    private val outputDir = requireNotNull(target.outputDir) { "Target is expected to have output directory: $target" }

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        inlinedTo.add(fromPath, jvmSignature, toPath)
    }

    public fun addDependentCache(cache: IncrementalCacheImpl) {
        dependents.add(cache)
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
            val classFilePath = getClassFilePath(className.internalName)

            fun addFilesAffectedByChangedInlineFuns(cache: IncrementalCacheImpl) {
                val targetFiles = functions.flatMap { cache.inlinedTo[classFilePath, it] }
                result.addAll(targetFiles)
            }

            addFilesAffectedByChangedInlineFuns(this)
            dependents.forEach(::addFilesAffectedByChangedInlineFuns)
        }

        return result.map { File(it) }
    }

    public fun cleanDirtyInlineFunctions() {
        dirtyInlineFunctionsMap.clean()
    }

    override fun getClassFilePath(internalClassName: String): String {
        return toSystemIndependentName(File(outputDir, "$internalClassName.class").canonicalPath)
    }

    public fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File): CompilationResult {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.process(jvmClassName, file.readBytes(), emptyArray<String>(), isPackage = false, checkChangesIsOpenPart = false)
        dirtyOutputClassesMap.notDirty(MODULE_MAPPING_FILE_NAME)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
        return CompilationResult.NO_CHANGES
    }

    public fun saveFileToCache(generatedClass: GeneratedJvmClass): CompilationResult {
        val sourceFiles: Collection<File> = generatedClass.sourceFiles
        val kotlinClass: LocalFileKotlinClass = generatedClass.outputClass
        val className = JvmClassName.byClassId(kotlinClass.classId)

        dirtyOutputClassesMap.notDirty(className.internalName)
        sourceFiles.forEach {
            sourceToClassesMap.add(it, className)
        }

        val header = kotlinClass.classHeader
        val changesInfo = when {
            header.isCompatibleFileFacadeKind() -> {
                assert(sourceFiles.size() == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)

                protoMap.process(kotlinClass, isPackage = true) +
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true)
            }
            header.isCompatibleMultifileClassKind() -> {
                val partNames = kotlinClass.classHeader.filePartClassNames?.toList()
                                ?: throw AssertionError("Multifile class has no parts: ${kotlinClass.className}")
                multifileClassFacadeMap.add(className, partNames)

                // TODO NO_CHANGES? (delegates only)
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true)
            }
            header.isCompatibleMultifileClassPartKind() -> {
                assert(sourceFiles.size() == 1) { "Multifile class part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)
                multifileClassPartMap.add(className.internalName, header.multifileClassName!!)

                protoMap.process(kotlinClass, isPackage = true) +
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true)
            }
            header.isCompatibleClassKind() && !header.isLocalClass -> {
                protoMap.process(kotlinClass, isPackage = false) +
                constantsMap.process(kotlinClass) +
                inlineFunctionsMap.process(kotlinClass, isPackage = false)
            }
            else -> CompilationResult.NO_CHANGES
        }

        changesInfo.logIfSomethingChanged(className)
        return changesInfo
    }

    private fun CompilationResult.logIfSomethingChanged(className: JvmClassName) {
        if (this == CompilationResult.NO_CHANGES) return

        KotlinBuilder.LOG.debug("$className is changed: $this")
    }

    public fun clearCacheForRemovedClasses(): CompilationResult {

        fun <T> T.getNonPrivateNames(nameResolver: NameResolver, vararg members: T.() -> List<MessageLite>) =
                members.flatMap { this.it().filterNot { it.isPrivate }.names(nameResolver) }.toSet()

        fun createChangeInfo(className: JvmClassName): ChangeInfo? {
            if (className.internalName == MODULE_MAPPING_FILE_NAME) return null

            val mapValue = protoMap.get(className) ?: return null

            return when {
                mapValue.isPackageFacade -> {
                    val packageData = JvmProtoBufUtil.readPackageDataFrom(mapValue.bytes, mapValue.strings)

                    val memberNames =
                            packageData.packageProto.getNonPrivateNames(
                                    packageData.nameResolver,
                                    ProtoBuf.Package::getFunctionList,
                                    ProtoBuf.Package::getPropertyList
                            )

                    ChangeInfo.Removed(className.packageFqName, memberNames)
                }
                else -> {
                    val classData = JvmProtoBufUtil.readClassDataFrom(mapValue.bytes, mapValue.strings)

                    val memberNames =
                            classData.classProto.getNonPrivateNames(
                                    classData.nameResolver,
                                    ProtoBuf.Class::getConstructorList,
                                    ProtoBuf.Class::getFunctionList,
                                    ProtoBuf.Class::getPropertyList
                            ) +
                            classData.classProto.enumEntryList.map { classData.nameResolver.getString(it) }.toSet()

                    ChangeInfo.Removed(className.fqNameForClassNameWithoutDollars, memberNames)
                }
            }
        }

        val dirtyClasses = dirtyOutputClassesMap
                                .getDirtyOutputClasses()
                                .map(JvmClassName::byInternalName)
                                .toList()

        val changes =
                if (IncrementalCompilation.isExperimental())
                    dirtyClasses.mapNotNull { createChangeInfo(it) }.asSequence()
                else
                    emptySequence<ChangeInfo>()

        val changesInfo = dirtyClasses.fold(CompilationResult(changes = changes)) { info, className ->
            val newInfo = CompilationResult(protoChanged = className in protoMap,
                                            constantsChanged = className in constantsMap)
            newInfo.logIfSomethingChanged(className)
            info + newInfo
        }

        dirtyClasses.forEach {
            protoMap.remove(it)
            packagePartMap.remove(it)
            multifileClassFacadeMap.remove(it)
            multifileClassPartMap.remove(it)
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

    override fun getObsoleteMultifileClasses(): Collection<String> {
        val obsoleteMultifileClasses = linkedSetOf<String>()
        for (dirtyClass in dirtyOutputClassesMap.getDirtyOutputClasses()) {
            val dirtyFacade = multifileClassPartMap.getFacadeName(dirtyClass) ?: continue
            obsoleteMultifileClasses.add(dirtyFacade)
        }
        KotlinBuilder.LOG.debug("Obsolete multifile class facades: $obsoleteMultifileClasses")
        return obsoleteMultifileClasses
    }

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? {
        val partNames = multifileClassFacadeMap.getMultifileClassParts(facadeInternalName) ?: return null
        return partNames.filter { !dirtyOutputClassesMap.isDirty(it) }
    }

    override fun getMultifileFacade(partInternalName: String): String? {
        return multifileClassPartMap.getFacadeName(partInternalName)
    }

    override fun getModuleMappingData(): ByteArray? {
        return protoMap[JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)]?.bytes
    }

    public override fun clean() {
        super.clean()
        cacheVersionProvider.normalVersion(target).clean()
        cacheVersionProvider.experimentalVersion(target).clean()
    }

    public fun cleanExperimental() {
        cacheVersionProvider.experimentalVersion(target).clean()
    }

    private inner class ProtoMap(storageFile: File) : BasicStringMap<ProtoMapValue>(storageFile, ProtoMapValueExternalizer) {

        public fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            val header = kotlinClass.classHeader
            val bytes = BitEncoding.decodeBytes(header.annotationData!!)
            return put(kotlinClass.className, bytes, header.strings!!, isPackage, checkChangesIsOpenPart = true)
        }

        public fun process(className: JvmClassName, data: ByteArray, strings: Array<String>, isPackage: Boolean, checkChangesIsOpenPart: Boolean): CompilationResult {
            return put(className, data, strings, isPackage, checkChangesIsOpenPart)
        }

        private fun put(
                className: JvmClassName, bytes: ByteArray, strings: Array<String>, isPackage: Boolean, checkChangesIsOpenPart: Boolean
        ): CompilationResult {
            val key = className.internalName
            val oldData = storage[key]
            val data = ProtoMapValue(isPackage, bytes, strings)

            if (oldData == null ||
                !Arrays.equals(bytes, oldData.bytes) ||
                !Arrays.equals(strings, oldData.strings) ||
                isPackage != oldData.isPackageFacade
            ) {
                storage[key] = data
            }

            if (oldData == null || !checkChangesIsOpenPart) return CompilationResult(protoChanged = true)

            val diff = difference(oldData, data)

            if (!IncrementalCompilation.isExperimental()) return CompilationResult(protoChanged = diff != DifferenceKind.NONE)

            val fqName = if (isPackage) className.packageFqName else className.fqNameForClassNameWithoutDollars

            val changes =
                    when (diff) {
                        is DifferenceKind.NONE -> emptySequence<ChangeInfo>()
                        is DifferenceKind.CLASS_SIGNATURE -> sequenceOf(ChangeInfo.SignatureChanged(fqName))
                        is DifferenceKind.MEMBERS -> sequenceOf(ChangeInfo.MembersChanged(fqName, diff.names))
                    }

            return CompilationResult(protoChanged = diff != DifferenceKind.NONE, changes = changes)
        }

        operator fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        operator fun get(className: JvmClassName): ProtoMapValue? =
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
                    val staticFinal = Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_PRIVATE
                    if (value != null && access and staticFinal == Opcodes.ACC_STATIC or Opcodes.ACC_FINAL) {
                        result[name] = value
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            return if (result.isEmpty()) null else result
        }

        operator fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        public fun process(kotlinClass: LocalFileKotlinClass): CompilationResult {
            return put(kotlinClass.className, getConstantsMap(kotlinClass.fileContents))
        }

        private fun put(className: JvmClassName, constantsMap: Map<String, Any>?): CompilationResult {
            val key = className.getInternalName()

            val oldMap = storage[key]
            if (oldMap == constantsMap) return CompilationResult.NO_CHANGES

            if (constantsMap != null) {
                storage[key] = constantsMap
            }
            else {
                storage.remove(key)
            }

            return CompilationResult(constantsChanged = true)
        }

        public fun remove(className: JvmClassName) {
            put(className, null)
        }

        override fun dumpValue(value: Map<String, Any>): String =
                value.dumpMap(Any::toString)
    }

    private inner class InlineFunctionsMap(storageFile: File) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
        private fun getInlineFunctionsMap(bytes: ByteArray): Map<String, Long> {
            val result = HashMap<String, Long>()

            val inlineFunctions = inlineFunctionsJvmNames(bytes)
            if (inlineFunctions.isEmpty()) return emptyMap()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val dummyClassWriter = ClassWriter(Opcodes.ASM5)

                    return object : MethodVisitor(Opcodes.ASM5, dummyClassWriter.visitMethod(0, name, desc, null, exceptions)) {
                        override fun visitEnd() {
                            val jvmName = name + desc
                            if (jvmName !in inlineFunctions) return

                            val dummyBytes = dummyClassWriter.toByteArray()!!
                            val hash = dummyBytes.md5()
                            result[jvmName] = hash
                        }
                    }
                }

            }, 0)

            return result
        }

        public fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            return put(kotlinClass.className, getInlineFunctionsMap(kotlinClass.fileContents), isPackage)
        }

        private fun put(className: JvmClassName, newMap: Map<String, Long>, isPackage: Boolean): CompilationResult {
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

            val changes =
                    if (IncrementalCompilation.isExperimental()) {
                        val fqName = if (isPackage) className.packageFqName else className.fqNameForClassNameWithoutDollars
                        // TODO get name in better way instead of using substringBefore
                        (added.asSequence() + changed.asSequence()).map { ChangeInfo.MembersChanged(fqName, listOf(it.substringBefore("("))) }
                    }
                    else {
                        emptySequence<ChangeInfo>()
                    }

            return CompilationResult(inlineChanged = changed.isNotEmpty(),
                                     inlineAdded = added.isNotEmpty(),
                                     changes = changes)
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

    private inner class MultifileClassFacadeMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        public fun add(facadeName: JvmClassName, partNames: Collection<String>) {
            storage[facadeName.internalName] = partNames
        }

        public fun getMultifileClassParts(facadeName: String): Collection<String>? = storage[facadeName]

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Collection<String>): String = value.dumpCollection()
    }

    private inner class MultifileClassPartMap(storageFile: File) : BasicStringMap<String>(storageFile, EnumeratorStringDescriptor.INSTANCE) {
        public fun add(partName: String, facadeName: String) {
            storage[partName] = facadeName
        }

        public fun getFacadeName(partName: String): String? {
            return storage.get(partName)
        }

        public fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: String): String = value
    }

    private inner class SourceToClassesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor.INSTANCE, StringCollectionExternalizer) {
        public fun clearOutputsForSource(sourceFile: File) {
            remove(sourceFile.absolutePath)
        }

        public fun add(sourceFile: File, className: JvmClassName) {
            storage.append(sourceFile.absolutePath, { out -> IOUtil.writeUTF(out, className.internalName) })
        }

        public operator fun get(sourceFile: File): Collection<JvmClassName> =
                storage[sourceFile.absolutePath].orEmpty().map { JvmClassName.byInternalName(it) }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()

        override fun clean() {
            storage.keys.forEach { remove(it) }
        }

        private fun remove(path: String) {
            storage.remove(path)
        }
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

        public fun isDirty(className: String): Boolean =
                storage.contains(className)

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class DirtyInlineFunctionsMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        public fun getEntries(): Map<JvmClassName, Collection<String>> =
            storage.keys.toMap(JvmClassName::byInternalName) { storage[it]!! }

        public fun put(className: JvmClassName, changedFunctions: List<String>) {
            storage[className.internalName] = changedFunctions
        }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()
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

        public operator fun get(sourcePath: String, jvmSignature: String): Collection<String> {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            return storage[key] ?: emptySet()
        }

        override fun dumpKey(key: PathFunctionPair): String =
            "(${key.path}, ${key.function})"

        override fun dumpValue(value: Collection<String>) =
            value.dumpCollection()
    }
}

sealed class ChangeInfo(val fqName: FqName) {
    open class MembersChanged(fqName: FqName, val names: Collection<String>) : ChangeInfo(fqName) {
        override fun toStringProperties(): String = super.toStringProperties() + ", names = $names"
    }

    class Removed(fqName: FqName, names: Collection<String>) : MembersChanged(fqName, names)

    class SignatureChanged(fqName: FqName) : ChangeInfo(fqName)


    protected open fun toStringProperties(): String = "fqName = $fqName"

    override fun toString(): String {
        return this.javaClass.simpleName + "(${toStringProperties()})"
    }
}

data class CompilationResult(
        val protoChanged: Boolean = false,
        val constantsChanged: Boolean = false,
        val inlineChanged: Boolean = false,
        val inlineAdded: Boolean = false,
        val changes: Sequence<ChangeInfo> = emptySequence()
) {
    companion object {
        public val NO_CHANGES: CompilationResult = CompilationResult()
    }

    operator fun plus(other: CompilationResult): CompilationResult =
            CompilationResult(protoChanged || other.protoChanged,
                        constantsChanged || other.constantsChanged,
                        inlineChanged || other.inlineChanged,
                        inlineAdded || other.inlineAdded,
                        changes + other.changes)
}

private class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget,
        private val paths: BuildDataPaths
) : StorageProvider<IncrementalCacheImpl>() {

    override fun equals(other: Any?) = other is KotlinIncrementalStorageProvider && target == other.target

    override fun hashCode() = target.hashCode()

    override fun createStorage(targetDataDir: File): IncrementalCacheImpl =
            IncrementalCacheImpl(target, paths)
}

fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): IncrementalCacheImpl =
        getStorage(target, KotlinIncrementalStorageProvider(target, dataPaths))

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
        "[${sorted().joinToString(", ", transform = Any::toString)}]"
