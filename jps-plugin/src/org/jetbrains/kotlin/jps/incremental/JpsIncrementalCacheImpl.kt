/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import gnu.trove.THashSet
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.inlineFunctionsJvmNames
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*

class JpsIncrementalCacheImpl(
        target: ModuleBuildTarget,
        paths: BuildDataPaths
) : IncrementalCacheImpl<ModuleBuildTarget>(paths.getTargetDataRoot(target), target.outputDir, target), StorageOwner {

    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))
    private val dirtyInlineFunctionsMap = registerMap(DirtyInlineFunctionsMap(DIRTY_INLINE_FUNCTIONS.storageFile))
    private val inlinedTo = registerMap(InlineFunctionsFilesMap(INLINED_TO.storageFile))

    override fun registerInline(fromPath: String, jvmSignature: String, toPath: String) {
        if (!IncrementalCompilation.isExperimental()) {
            inlinedTo.add(fromPath, jvmSignature, toPath)
        }
    }

    override fun debugLog(message: String) {
        KotlinBuilder.LOG.debug(message)
    }

    override fun additionalProcessChangedClass(kotlinClass: LocalFileKotlinClass, isPackage: Boolean) =
            inlineFunctionsMap.process(kotlinClass, isPackage)

    override fun additionalProcessRemovedClasses(dirtyClasses: List<JvmClassName>) {
        dirtyClasses.forEach { inlineFunctionsMap.remove(it) }
    }

    fun getFilesToReinline(): Collection<File> {
        val result = THashSet(FileUtil.PATH_HASHING_STRATEGY)

        for ((className, functions) in dirtyInlineFunctionsMap.getEntries()) {
            val classFilePath = getClassFilePath(className.internalName)

            for (cache in dependentsWithThis) {
                val targetFiles = functions.flatMap { (cache as JpsIncrementalCacheImpl).inlinedTo[classFilePath, it] }
                result.addAll(targetFiles)
            }
        }

        return result.map { File(it) }
    }

    fun cleanDirtyInlineFunctions() {
        dirtyInlineFunctionsMap.clean()
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

        fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            return put(kotlinClass.className, getInlineFunctionsMap(kotlinClass.fileContents), isPackage)
        }

        private fun put(className: JvmClassName, newMap: Map<String, Long>, isPackage: Boolean): CompilationResult {
            val internalName = className.internalName
            val oldMap = storage[internalName] ?: emptyMap()

            val added = hashSetOf<String>()
            val changed = hashSetOf<String>()
            val allFunctions = oldMap.keys + newMap.keys

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

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
                value.dumpMap { java.lang.Long.toHexString(it) }
    }

    private inner class DirtyInlineFunctionsMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        fun getEntries(): Map<JvmClassName, Collection<String>> =
                storage.keys.toMapBy(JvmClassName::byInternalName) { storage[it]!! }

        fun put(className: JvmClassName, changedFunctions: List<String>) {
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
    private inner class InlineFunctionsFilesMap(storageFile: File) : BasicMap<PathFunctionPair, Collection<String>>(storageFile, PathFunctionPairKeyDescriptor, PathStringCollectionExternalizer) {
        fun add(sourcePath: String, jvmSignature: String, targetPath: String) {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            storage.append(key, targetPath)
        }

        operator fun get(sourcePath: String, jvmSignature: String): Collection<String> {
            val key = PathFunctionPair(sourcePath, jvmSignature)
            return storage[key] ?: emptySet()
        }

        override fun dumpKey(key: PathFunctionPair): String =
                "(${key.path}, ${key.function})"

        override fun dumpValue(value: Collection<String>) =
                value.dumpCollection()
    }

    companion object {
        private val INLINE_FUNCTIONS = "inline-functions"
        private val DIRTY_INLINE_FUNCTIONS = "dirty-inline-functions"
        private val INLINED_TO = "inlined-to"
    }
}

private class KotlinIncrementalStorageProvider(
        private val target: ModuleBuildTarget,
        private val paths: BuildDataPaths
) : StorageProvider<JpsIncrementalCacheImpl>() {

    override fun equals(other: Any?) = other is KotlinIncrementalStorageProvider && target == other.target

    override fun hashCode() = target.hashCode()

    override fun createStorage(targetDataDir: File): JpsIncrementalCacheImpl =
            JpsIncrementalCacheImpl(target, paths)
}

fun BuildDataManager.getKotlinCache(target: ModuleBuildTarget): JpsIncrementalCacheImpl =
        getStorage(target, KotlinIncrementalStorageProvider(target, dataPaths))

