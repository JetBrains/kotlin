/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.incremental

import org.jetbrains.jet.jps.incremental.IncrementalCacheImpl.RecompilationDecision.*
import java.io.File
import com.intellij.util.io.PersistentHashMap
import java.io.DataOutput
import com.intellij.util.io.IOUtil
import java.io.DataInput
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.util.io.DataExternalizer
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.descriptors.serialization.BitEncoding
import org.jetbrains.jet.utils.intellij.*
import java.util.Arrays
import org.jetbrains.org.objectweb.asm.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalCache
import java.util.HashMap
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import com.intellij.util.containers.MultiMap
import com.intellij.openapi.util.io.FileUtil
import java.security.MessageDigest

val INLINE_ANNOTATION_DESC = "Lkotlin/inline;"

public class IncrementalCacheImpl(val baseDir: File): IncrementalCache {
    class object {
        val PROTO_MAP = "proto.tab"
        val CONSTANTS_MAP = "constants.tab"
        val INLINE_FUNCTIONS = "inline-functions.tab"
        val PACKAGE_PARTS = "package-parts.tab"
    }

    private val protoMap = ProtoMap()
    private val constantsMap = ConstantsMap()
    private val inlineFunctionsMap = InlineFunctionsMap()
    private val packagePartMap = PackagePartMap()

    public fun saveFileToCache(moduleId: String, sourceFiles: Collection<File>, classFile: File): RecompilationDecision {
        val fileBytes = classFile.readBytes()
        val classNameAndHeader = VirtualFileKotlinClass.readClassNameAndHeader(fileBytes)
        if (classNameAndHeader == null) return RecompilationDecision.DO_NOTHING

        val (className, header) = classNameAndHeader
        val annotationDataEncoded = header.annotationData
        if (annotationDataEncoded != null) {
            val data = BitEncoding.decodeBytes(annotationDataEncoded)
            when (header.kind) {
                KotlinClassHeader.Kind.PACKAGE_FACADE -> {
                    return if (protoMap.put(moduleId, className, data)) COMPILE_OTHERS else DO_NOTHING
                }
                KotlinClassHeader.Kind.CLASS -> {
                    val inlinesChanged = inlineFunctionsMap.process(moduleId, className, fileBytes)
                    val protoChanged = protoMap.put(moduleId, className, data)
                    val constantsChanged = constantsMap.process(moduleId, className, fileBytes)

                    return if (inlinesChanged) RECOMPILE_ALL else if (protoChanged || constantsChanged) COMPILE_OTHERS else DO_NOTHING
                }
                else -> {
                    throw IllegalStateException("Unexpected kind with annotationData: ${header.kind}")
                }
            }
        }
        if (header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART) {
            assert(sourceFiles.size == 1) { "Package part from several source files: $sourceFiles" }
            packagePartMap.putPackagePartSourceData(moduleId, sourceFiles.first(), className)
            val inlinesChanged = inlineFunctionsMap.process(moduleId, className, fileBytes)
            val constantsChanged = constantsMap.process(moduleId, className, fileBytes)
            return if (inlinesChanged) RECOMPILE_ALL else if (constantsChanged) COMPILE_OTHERS else DO_NOTHING
        }

        return DO_NOTHING
    }

    public fun clearCacheForRemovedFiles(moduleIdsAndSourceFiles: Collection<Pair<String, File>>, outDirectories: Map<String, File>) {
        for ((moduleId, sourceFile) in moduleIdsAndSourceFiles) {
            packagePartMap.remove(moduleId, sourceFile)
        }

        inlineFunctionsMap.clearOutdated(outDirectories)
        constantsMap.clearOutdated(outDirectories)
        protoMap.clearOutdated(outDirectories)
    }

    public override fun getRemovedPackageParts(moduleId: String, compiledSourceFilesToFqName: Map<File, String>): Collection<String> {
        return packagePartMap.getRemovedPackageParts(moduleId, compiledSourceFilesToFqName)
    }

    public override fun getPackageData(moduleId: String, fqName: String): ByteArray? {
        return protoMap[moduleId, JvmClassName.byFqNameWithoutInnerClasses(PackageClassUtils.getPackageClassFqName(FqName(fqName)))]
    }

    public fun close() {
        protoMap.close()
        constantsMap.close()
        inlineFunctionsMap.close()
        packagePartMap.close()
    }

    private abstract class ClassFileBasedMap {
        protected abstract val map: PersistentHashMap<String, *>

        protected fun getKeyString(moduleId: String, className: JvmClassName): String {
            return moduleId + ":" + className.getInternalName()
        }

        protected fun parseKeyString(key: String): Pair<String, JvmClassName> {
            val colon = key.lastIndexOf(":")
            return Pair(key.substring(0, colon), JvmClassName.byInternalName(key.substring(colon + 1)))
        }

        public fun clearOutdated(outDirectories: Map<String, File>) {
            val keysToRemove = HashSet<String>()

            map.processKeys { key ->
                val (moduleId, className) = parseKeyString(key!!)
                val outDir = outDirectories[moduleId]
                if (outDir != null) {
                    val classFile = File(outDir, FileUtil.toSystemDependentName(className.getInternalName()) + ".class")
                    if (!classFile.exists()) {
                        keysToRemove.add(key)
                    }
                }

                true
            }

            for (key in keysToRemove) {
                map.remove(key)
            }
        }

        public fun close() {
            map.close()
        }
    }

    private inner class ProtoMap: ClassFileBasedMap() {
        override val map: PersistentHashMap<String, ByteArray> = PersistentHashMap(
                File(baseDir, PROTO_MAP),
                EnumeratorStringDescriptor(),
                ByteArrayExternalizer
        )

        public fun put(moduleId: String, className: JvmClassName, data: ByteArray): Boolean {
            val key = getKeyString(moduleId, className)
            val oldData = map[key]
            if (Arrays.equals(data, oldData)) {
                return false
            }
            map.put(key, data)
            return true
        }

        public fun get(moduleId: String, className: JvmClassName): ByteArray? {
            return map[getKeyString(moduleId, className)]
        }
    }

    private inner class ConstantsMap: ClassFileBasedMap() {
        override val map: PersistentHashMap<String, Map<String, Any>> = PersistentHashMap(
                File(baseDir, CONSTANTS_MAP),
                EnumeratorStringDescriptor(),
                ConstantsMapExternalizer
        )

        private fun getConstantsMap(bytes: ByteArray): Map<String, Any> {
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

            return result
        }

        public fun process(moduleId: String, className: JvmClassName, bytes: ByteArray): Boolean {
            return put(moduleId, className, getConstantsMap(bytes))
        }

        private fun put(moduleId: String, className: JvmClassName, constantsMap: Map<String, Any>): Boolean {
            val key = getKeyString(moduleId, className)

            val oldMap = map[key]
            if (oldMap == constantsMap) {
                return false
            }
            map.put(key, constantsMap)
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

    private inner class InlineFunctionsMap: ClassFileBasedMap() {
        override val map: PersistentHashMap<String, Map<String, Long>> = PersistentHashMap(
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

        public fun process(moduleId: String, className: JvmClassName, bytes: ByteArray): Boolean {
            return put(moduleId, className, getInlineFunctionsMap(bytes))
        }

        private fun put(moduleId: String, className: JvmClassName, inlineFunctionsMap: Map<String, Long>?): Boolean {
            val key = getKeyString(moduleId, className)

            val oldMap = map[key]
            if (oldMap == inlineFunctionsMap) {
                return false
            }
            if (inlineFunctionsMap != null) {
                map.put(key, inlineFunctionsMap)
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


    private inner class PackagePartMap {
        // Format of serialization to string: <module id> <path separator> <source file path>  -->  <package part JVM internal name>
        private val map: PersistentHashMap<String, String> = PersistentHashMap(
                File(baseDir, PACKAGE_PARTS),
                EnumeratorStringDescriptor(),
                EnumeratorStringDescriptor()
        )

        private fun getKey(moduleId: String, sourceFile: File): String {
            return moduleId + File.pathSeparator + sourceFile.getAbsolutePath()
        }

        public fun putPackagePartSourceData(moduleId: String, sourceFile: File, className: JvmClassName) {
            map.put(getKey(moduleId, sourceFile), className.getInternalName())
        }

        public fun remove(moduleId: String, sourceFile: File) {
            map.remove(getKey(moduleId, sourceFile))
        }

        public fun getRemovedPackageParts(moduleId: String, compiledSourceFilesToFqName: Map<File, String>): Collection<String> {
            val result = HashSet<String>()

            map.processKeysWithExistingMapping { key ->
                if (key!!.startsWith(moduleId + File.pathSeparator)) {
                    val sourceFile = File(key.substring(moduleId.length + 1))

                    val packagePartClassName = map[key]!!
                    if (!sourceFile.exists()) {
                        result.add(packagePartClassName)
                    }
                    else {
                        val previousPackageFqName = JvmClassName.byInternalName(packagePartClassName).getFqNameForClassNameWithoutDollars().parent()
                        val currentPackageFqName = compiledSourceFilesToFqName[sourceFile]
                        if (currentPackageFqName != null && currentPackageFqName != previousPackageFqName.asString()) {
                            result.add(packagePartClassName)
                        }
                    }
                }

                true
            }

            return result
        }

        public fun getModulesToPackages(): MultiMap<String, FqName> {
            val result = MultiMap.createSet<String, FqName>()

            map.processKeysWithExistingMapping { key ->
                val indexOf = key!!.indexOf(File.pathSeparator)
                val moduleId = key.substring(0, indexOf)
                val packagePartClassName = map[key]!!

                val packageFqName = JvmClassName.byInternalName(packagePartClassName).getFqNameForClassNameWithoutDollars().parent()

                result.putValue(moduleId, packageFqName)

                true
            }

            return result
        }

        public fun close() {
            map.close()
        }
    }

    enum class RecompilationDecision {
        DO_NOTHING
        COMPILE_OTHERS
        RECOMPILE_ALL

        fun merge(other: RecompilationDecision): RecompilationDecision {
            return if (other.ordinal() > this.ordinal()) other else this
        }
    }
}

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
    override fun save(out: DataOutput, value: ByteArray?) {
        out.writeInt(value!!.size)
        out.write(value)
    }

    override fun read(`in`: DataInput): ByteArray {
        val length = `in`.readInt()
        val buf = ByteArray(length)
        `in`.readFully(buf)
        return buf
    }
}
