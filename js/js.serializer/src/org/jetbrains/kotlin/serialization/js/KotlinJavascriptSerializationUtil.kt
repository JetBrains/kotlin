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

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KotlinJavascriptSerializationUtil {
    val CLASS_METADATA_FILE_EXTENSION: String = "kjsm"

    private val PACKAGE_DEFAULT_BYTES = run {
        val stream = ByteArrayOutputStream()
        ProtoBuf.Package.getDefaultInstance().writeTo(stream)
        stream.toByteArray()
    }

    private val CLASSES_IN_PACKAGE_DEFAULT_BYTES = run {
        val stream = ByteArrayOutputStream()
        JsProtoBuf.Classes.getDefaultInstance().writeTo(stream)
        stream.toByteArray()
    }

    private val STRING_TABLE_DEFAULT_BYTES = run {
        val serializer = DescriptorSerializer.createTopLevel(KotlinJavascriptSerializerExtension())
        val stream = ByteArrayOutputStream()
        serializer.stringTable.serializeTo(stream)
        stream.toByteArray()
    }

    @JvmStatic fun createPackageFragmentProvider(moduleDescriptor: ModuleDescriptor, metadata: ByteArray, storageManager: StorageManager): PackageFragmentProvider? {
        val contentMap = metadata.toContentMap()

        val packageFqNames = getPackages(contentMap).map { FqName(it) }.toSet()
        if (packageFqNames.isEmpty()) return null

        return createKotlinJavascriptPackageFragmentProvider(storageManager, moduleDescriptor, packageFqNames) {
            path ->
            if (!contentMap.containsKey(path)) {
                when {
                    isPackageMetadataFile(path) ->
                        ByteArrayInputStream(PACKAGE_DEFAULT_BYTES)
                    isStringTableFile(path) ->
                        ByteArrayInputStream(STRING_TABLE_DEFAULT_BYTES)
                    isClassesInPackageFile(path) ->
                        ByteArrayInputStream(CLASSES_IN_PACKAGE_DEFAULT_BYTES)
                    else ->
                        null
                }
            }
            else ByteArrayInputStream(contentMap.get(path))
        }
    }

    fun contentMapToByteArray(contentMap: Map<String, ByteArray>): ByteArray {
        val contentBuilder = JsProtoBuf.Library.newBuilder()
        contentMap.forEach {
            val entry = JsProtoBuf.Library.FileEntry.newBuilder().setPath(it.key).setContent(ByteString.copyFrom(it.value)).build()
            contentBuilder.addEntry(entry)
        }

        val byteStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteStream)
        contentBuilder.build().writeTo(gzipOutputStream)
        gzipOutputStream.close()

        return byteStream.toByteArray()
    }

    fun metadataAsString(moduleName: String, moduleDescriptor: ModuleDescriptor): String =
        KotlinJavascriptMetadataUtils.formatMetadataAsString(moduleName, moduleDescriptor.toBinaryMetadata())

    fun serializePackage(module: ModuleDescriptor, fqName: FqName, writeFun: (String, ByteArray) -> Unit) {
        val packageView = module.getPackage(fqName)

        val skip: (DeclarationDescriptor) -> Boolean = { DescriptorUtils.getContainingModule(it) != module }

        val serializerExtension = KotlinJavascriptSerializerExtension()
        val serializer = DescriptorSerializer.createTopLevel(serializerExtension)

        val classifierDescriptors = DescriptorSerializer.sort(packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))

        ClassSerializationUtil.serializeClasses(classifierDescriptors, serializer, object : ClassSerializationUtil.Sink {
            override fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class) {
                val stream = ByteArrayOutputStream()
                classProto.writeTo(stream)
                writeFun(getFileName(classDescriptor), stream.toByteArray())
            }
        }, skip)

        val packageStream = ByteArrayOutputStream()
        val fragments = packageView.fragments
        val packageProto = serializer.packageProto(fragments, skip).build() ?: error("Package fragments not serialized: $fragments")
        if (packageProto.functionCount > 0 || packageProto.propertyCount > 0) {
            packageProto.writeTo(packageStream)
            writeFun(KotlinJavascriptSerializedResourcePaths.getPackageFilePath(fqName), packageStream.toByteArray())
        }

        val strings = serializerExtension.stringTable
        serializeClassNamesInPackage(fqName, fragments, strings, skip, writeFun)

        val nameStream = ByteArrayOutputStream()
        strings.serializeTo(nameStream)
        val stringBytes = nameStream.toByteArray()

        if (!stringBytes.isEmpty() && !Arrays.equals(stringBytes, STRING_TABLE_DEFAULT_BYTES)) {
            writeFun(KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(fqName), stringBytes)
        }
    }

    private fun serializeClassNamesInPackage(
            fqName: FqName,
            packageFragments: Collection<PackageFragmentDescriptor>,
            stringTable: StringTableImpl,
            skip: (DeclarationDescriptor) -> Boolean,
            writeFun: (String, ByteArray) -> Unit
    ) {
        val classes = packageFragments.flatMap {
            it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassDescriptor>()
        }.filter { !skip(it) }

        val builder = JsProtoBuf.Classes.newBuilder()

        for (descriptor in DescriptorSerializer.sort(classes)) {
            builder.addClassName(stringTable.getSimpleNameIndex(descriptor.name))
        }

        val classesProto = builder.build()

        if (classesProto.classNameCount > 0) {
            val stream = ByteArrayOutputStream()
            classesProto.writeTo(stream)
            writeFun(KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(fqName), stream.toByteArray())
        }
    }

    private fun getFileName(classDescriptor: ClassDescriptor): String {
        return KotlinJavascriptSerializedResourcePaths.getClassMetadataPath(classDescriptor.classId)
    }

    fun toContentMap(module: ModuleDescriptor): Map<String, ByteArray> {
        val contentMap = hashMapOf<String, ByteArray>()

        getPackagesFqNames(module).forEach {
            serializePackage(module, it) {
                fileName, bytes -> contentMap[fileName] = bytes
            }
        }

        return contentMap
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        return HashSet<FqName>().apply {
            getSubPackagesFqNames(module.getPackage(FqName.ROOT), this)
            add(FqName.ROOT)
        }
    }

    private fun getSubPackagesFqNames(packageView: PackageViewDescriptor, result: MutableSet<FqName>) {
        val fqName = packageView.fqName
        if (!fqName.isRoot) {
            result.add(fqName)
        }

        for (descriptor in packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)) {
            if (descriptor is PackageViewDescriptor) {
                getSubPackagesFqNames(descriptor, result)
            }
        }
    }

    private fun getPackages(contentMap: Map<String, ByteArray>): Set<String> {
        val keys = contentMap.keys.map { (if (it.startsWith('/')) it else "/" + it).substringBeforeLast('/') }.toSet()

        val result = hashSetOf<String>()

        fun addNames(name: String) {
            result.add(name)
            if (name != "") {
                addNames(name.substringBeforeLast('/'))
            }
        }

        keys.forEach { addNames(it) }

        return result.map { it.substringAfter('/').replace('/', '.') }.toSet()
    }

    private fun ModuleDescriptor.toBinaryMetadata(): ByteArray =
            KotlinJavascriptSerializationUtil.contentMapToByteArray(toContentMap(this))
}

fun KotlinJavascriptMetadata.forEachFile(operation: (filePath: String, fileContent: ByteArray) -> Unit): Unit =
        this.body.toContentMap().forEach { operation(it.key, it.value) }

private fun ByteArray.toContentMap(): Map<String, ByteArray> {
    val gzipInputStream = GZIPInputStream(ByteArrayInputStream(this))
    val content = JsProtoBuf.Library.parseFrom(gzipInputStream)
    gzipInputStream.close()

    val contentMap: MutableMap<String, ByteArray> = hashMapOf()
    content.entryList.forEach { entry -> contentMap[entry.path] = entry.content.toByteArray() }

    return contentMap
}