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

import com.google.protobuf.ByteString
import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import org.jetbrains.kotlin.builtins.BuiltinsPackageFragment
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializationUtil
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializerExtension
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.platform.platformStatic

public object KotlinJavascriptSerializationUtil {
    private val PACKAGE_FILE_SUFFIX = "/.kotlin_package"

    platformStatic
    public fun getPackageFragmentProviders(moduleDescriptor: ModuleDescriptor, metadata: ByteArray): List<PackageFragmentProvider> {
        val gzipInputStream = GZIPInputStream(ByteArrayInputStream(metadata))
        val content = JsProtoBuf.Library.parseFrom(gzipInputStream)
        gzipInputStream.close()

        val contentMap: MutableMap<String, ByteArray> = hashMapOf()
        for(index in 0..content.getEntryCount()-1) {
            val entry = content.getEntry(index)
            contentMap[entry.getPath()] = entry.getContent().toByteArray()
        }

        val packages = getPackages(contentMap)

        val load = { (path: String) -> if (!contentMap.containsKey(path)) null else ByteArrayInputStream(contentMap.get(path)) }

        val providers = arrayListOf<PackageFragmentProvider>()
        for (packageName in packages) {
            val fqName = FqName(packageName)
            val packageFragment = BuiltinsPackageFragment(fqName, LockBasedStorageManager(), moduleDescriptor, FlexibleTypeCapabilitiesDeserializer.Dynamic, load)
            providers.add(packageFragment.provider)
        }

        return providers
    }

    public fun contentMapToByteArray(contentMap: Map<String, ByteArray>): ByteArray {
        val contentBuilder = JsProtoBuf.Library.newBuilder()
        contentMap forEach {
            val entry = JsProtoBuf.Library.FileEntry.newBuilder().setPath(it.getKey()).setContent(ByteString.copyFrom(it.getValue())).build()
            contentBuilder.addEntry(entry)
        }

        val byteStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteStream)
        contentBuilder.build().writeTo(gzipOutputStream)
        gzipOutputStream.close()

        return byteStream.toByteArray()
    }

    public fun metadataAsString(moduleName: String, moduleDescriptor: ModuleDescriptor): String {
        val contentMap = hashMapOf<String, ByteArray>()

        DescriptorUtils.getPackagesFqNames(moduleDescriptor).forEach {
            fqName ->
            serializePackage(moduleDescriptor, fqName) {
                fileName, stream ->
                contentMap[fileName] = stream.toByteArray()
            }
        }

        val content = KotlinJavascriptSerializationUtil.contentMapToByteArray(contentMap)
        return KotlinJavascriptMetadataUtils.formatMetadataAsString(moduleName, content)
    }

    fun serializePackage(module: ModuleDescriptor, fqName: FqName, writeFun: (String, ByteArrayOutputStream) -> Unit) {
        val packageView = module.getPackage(fqName) ?: error("No package resolved in $module")

        val skip: (DeclarationDescriptor) -> Boolean = { DescriptorUtils.getContainingModule(it) != module }

        val serializer = DescriptorSerializer.createTopLevel(BuiltInsSerializerExtension)

        val classifierDescriptors = DescriptorSerializer.sort(packageView.getMemberScope().getDescriptors(DescriptorKindFilter.CLASSIFIERS))

        ClassSerializationUtil.serializeClasses(classifierDescriptors, serializer, object : ClassSerializationUtil.Sink {
            override fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class) {
                val stream = ByteArrayOutputStream()
                classProto.writeTo(stream)
                writeFun(getFileName(classDescriptor), stream)
            }
        }, skip)

        val packageStream = ByteArrayOutputStream()
        val fragments = module.getPackageFragmentProvider().getPackageFragments(fqName)
        val packageProto = serializer.packageProto(fragments, skip).build() ?: error("Package fragments not serialized: $fragments")
        packageProto.writeTo(packageStream)
        writeFun(BuiltInsSerializationUtil.getPackageFilePath(fqName), packageStream)

        val nameStream = ByteArrayOutputStream()
        val strings = serializer.getStringTable()
        SerializationUtil.serializeStringTable(nameStream, strings.serializeSimpleNames(), strings.serializeQualifiedNames())
        writeFun(BuiltInsSerializationUtil.getStringTableFilePath(fqName), nameStream)
    }

    fun getFileName(classDescriptor: ClassDescriptor): String {
        return BuiltInsSerializationUtil.getClassMetadataPath(classDescriptor.classId)
    }

    private fun getPackages(contentMap: Map<String, ByteArray>): List<String> =
            contentMap.keySet().filter { it.endsWith(PACKAGE_FILE_SUFFIX) } map { it.substring(0, it.length() - PACKAGE_FILE_SUFFIX.length()).replace('/', '.') }
}