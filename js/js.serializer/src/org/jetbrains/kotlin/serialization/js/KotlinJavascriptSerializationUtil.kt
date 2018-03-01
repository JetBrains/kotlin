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

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isPreRelease
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KotlinJavascriptSerializationUtil {
    val CLASS_METADATA_FILE_EXTENSION: String = "kjsm"

    @JvmStatic
    fun readModule(
            metadata: ByteArray,
            storageManager: StorageManager,
            module: ModuleDescriptor,
            configuration: DeserializationConfiguration,
            lookupTracker: LookupTracker
    ): JsModuleDescriptor<PackageFragmentProvider?> =
        readModuleFromProto(readModuleAsProto(metadata, module.name.asString()), storageManager, module, configuration, lookupTracker)

    @JvmStatic
    fun readModuleAsProto(metadata: ByteArray, name: String): JsModuleDescriptor<KotlinJavaScriptLibraryParts> =
        metadata.deserializeToLibraryParts(name)

    @JvmStatic
    fun readModuleFromProto(
            jsModule: JsModuleDescriptor<KotlinJavaScriptLibraryParts>,
            storageManager: StorageManager, module: ModuleDescriptor,
            configuration: DeserializationConfiguration,
            lookupTracker: LookupTracker
    ): JsModuleDescriptor<PackageFragmentProvider?> {
        val (header, packageFragmentProtos) = jsModule.data
        return jsModule.copy(createKotlinJavascriptPackageFragmentProvider(
                storageManager, module, header, packageFragmentProtos, configuration, lookupTracker
        ))
    }

    fun readDescriptors(
            metadata: PackagesWithHeaderMetadata,
            storageManager: StorageManager,
            module: ModuleDescriptor,
            configuration: DeserializationConfiguration,
            lookupTracker: LookupTracker
    ): PackageFragmentProvider {
        val scopeProto = metadata.packages.map {
            ProtoBuf.PackageFragment.parseFrom(it, JsSerializerProtocol.extensionRegistry)
        }
        val headerProto = JsProtoBuf.Header.parseFrom(CodedInputStream.newInstance(metadata.header), JsSerializerProtocol.extensionRegistry)
        return createKotlinJavascriptPackageFragmentProvider(storageManager, module, headerProto, scopeProto, configuration, lookupTracker)
    }

    fun serializeMetadata(
            bindingContext: BindingContext,
            module: ModuleDescriptor,
            moduleKind: ModuleKind,
            importedModules: List<String>
    ): JsProtoBuf.Library {
        val builder = JsProtoBuf.Library.newBuilder()

        val moduleProtoKind = when (moduleKind) {
            ModuleKind.PLAIN -> JsProtoBuf.Library.Kind.PLAIN
            ModuleKind.AMD -> JsProtoBuf.Library.Kind.AMD
            ModuleKind.COMMON_JS -> JsProtoBuf.Library.Kind.COMMON_JS
            ModuleKind.UMD -> JsProtoBuf.Library.Kind.UMD
        }
        if (builder.kind != moduleProtoKind) {
            builder.kind = moduleProtoKind
        }

        importedModules.forEach { builder.addImportedModule(it) }

        for (fqName in getPackagesFqNames(module)) {
            val fragment = serializePackageFragment(bindingContext, module, fqName)
            if (!fragment.isEmpty()) {
                builder.addPackageFragment(fragment)
            }
        }

        return builder.build()
    }

    fun metadataAsString(
            bindingContext: BindingContext,
            jsDescriptor: JsModuleDescriptor<ModuleDescriptor>,
            languageVersionSettings: LanguageVersionSettings
    ): String = KotlinJavascriptMetadataUtils.formatMetadataAsString(
            jsDescriptor.name,
            jsDescriptor.serializeToBinaryMetadata(bindingContext, languageVersionSettings)
    )

    private fun serializePackageFragment(bindingContext: BindingContext, module: ModuleDescriptor, fqName: FqName): ProtoBuf.PackageFragment {
        val packageView = module.getPackage(fqName)
        return serializeDescriptors(bindingContext, module, packageView.memberScope.getContributedDescriptors(), fqName)
    }

    fun serializeDescriptors(
            bindingContext: BindingContext,
            module: ModuleDescriptor,
            scope: Collection<DeclarationDescriptor>,
            fqName: FqName
    ): ProtoBuf.PackageFragment {
        val builder = ProtoBuf.PackageFragment.newBuilder()

        // TODO: ModuleDescriptor should be able to return the package only with the contents of that module, without dependencies
        val skip: (DeclarationDescriptor) -> Boolean = {
            DescriptorUtils.getContainingModule(it) != module || (it is MemberDescriptor && it.isExpect)
        }

        val fileRegistry = KotlinFileRegistry()
        val serializerExtension = KotlinJavascriptSerializerExtension(fileRegistry)
        val serializer = DescriptorSerializer.createTopLevel(serializerExtension)

        val classDescriptors = DescriptorSerializer.sort(scope).filterIsInstance<ClassDescriptor>()

        fun serializeClasses(descriptors: Collection<DeclarationDescriptor>) {
            fun serializeClass(classDescriptor: ClassDescriptor) {
                if (skip(classDescriptor)) return
                val classProto = serializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
                builder.addClass_(classProto)
                serializeClasses(classDescriptor.unsubstitutedInnerClassesScope.getContributedDescriptors())
            }

            for (descriptor in descriptors) {
                if (descriptor is ClassDescriptor) {
                    serializeClass(descriptor)
                }
            }
        }

        serializeClasses(classDescriptors)

        val stringTable = serializerExtension.stringTable

        val members = scope.filterNot(skip)
        builder.`package` = serializer.packagePartProto(fqName, members).build()

        builder.setExtension(
                JsProtoBuf.packageFragmentFiles,
                serializeFiles(fileRegistry, bindingContext, AnnotationSerializer(stringTable))
        )

        val (strings, qualifiedNames) = stringTable.buildProto()
        builder.strings = strings
        builder.qualifiedNames = qualifiedNames

        return builder.build()
    }

    private fun serializeFiles(
            fileRegistry: KotlinFileRegistry,
            bindingContext: BindingContext,
            serializer: AnnotationSerializer
    ): JsProtoBuf.Files {
        val filesProto = JsProtoBuf.Files.newBuilder()
        for ((file, id) in fileRegistry.fileIds.entries.sortedBy { it.value }) {
            val fileProto = JsProtoBuf.File.newBuilder()
            if (id != filesProto.fileCount) {
                fileProto.id = id
            }
            val annotations = when (file) {
                is KotlinPsiFileMetadata -> file.ktFile.annotationEntries.map { bindingContext[BindingContext.ANNOTATION, it]!! }
                is KotlinDeserializedFileMetadata -> file.packageFragment.fileMap[file.fileId]!!.annotations
            }
            for (annotation in annotations) {
                fileProto.addAnnotation(serializer.serializeAnnotation(annotation))
            }
            filesProto.addFile(fileProto)
        }
        return filesProto.build()
    }

    fun toContentMap(
            bindingContext: BindingContext,
            module: ModuleDescriptor,
            languageVersionSettings: LanguageVersionSettings
    ): Map<String, ByteArray> {
        val contentMap = mutableMapOf<String, ByteArray>()

        for (fqName in getPackagesFqNames(module)) {
            val part = serializePackageFragment(bindingContext, module, fqName)
            if (part.isEmpty()) continue

            val stream = ByteArrayOutputStream()
            with(DataOutputStream(stream)) {
                val version = JsMetadataVersion.INSTANCE.toArray()
                writeInt(version.size)
                version.forEach(this::writeInt)
            }

            serializeHeader(module, fqName, languageVersionSettings).writeDelimitedTo(stream)
            part.writeTo(stream)

            contentMap[JsSerializerProtocol.getKjsmFilePath(fqName)] = stream.toByteArray()
        }

        return contentMap
    }

    private fun ProtoBuf.PackageFragment.isEmpty(): Boolean =
            class_Count == 0 && `package`.let { it.functionCount == 0 && it.propertyCount == 0 && it.typeAliasCount == 0 }

    fun serializeHeader(
        module: ModuleDescriptor, packageFqName: FqName?, languageVersionSettings: LanguageVersionSettings
    ): JsProtoBuf.Header {
        val header = JsProtoBuf.Header.newBuilder()

        if (packageFqName != null) {
            header.packageFqName = packageFqName.asString()
        }

        if (languageVersionSettings.isPreRelease()) {
            header.flags = 1
        }

        val experimentalAnnotationFqNames = languageVersionSettings.getFlag(AnalysisFlag.experimental)
        if (experimentalAnnotationFqNames.isNotEmpty()) {
            val stringTable = StringTableImpl()
            for (fqName in experimentalAnnotationFqNames) {
                val descriptor = module.resolveClassByFqName(FqName(fqName), NoLookupLocation.FOR_ALREADY_TRACKED) ?: continue
                header.addAnnotation(ProtoBuf.Annotation.newBuilder().apply {
                    id = stringTable.getFqNameIndex(descriptor)
                })
            }
            val (strings, qualifiedNames) = stringTable.buildProto()
            header.strings = strings
            header.qualifiedNames = qualifiedNames
        }

        // TODO: write JS code binary version

        return header.build()
    }

    private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        return mutableSetOf<FqName>().apply {
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

    private fun JsModuleDescriptor<ModuleDescriptor>.serializeToBinaryMetadata(
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ): ByteArray {
        return ByteArrayOutputStream().apply {
            GZIPOutputStream(this).use { stream ->
                serializeHeader(data, null, languageVersionSettings).writeDelimitedTo(stream)
                serializeMetadata(bindingContext, data, kind, imported).writeTo(stream)
            }
        }.toByteArray()
    }

    private fun ByteArray.deserializeToLibraryParts(name: String): JsModuleDescriptor<KotlinJavaScriptLibraryParts> {
        val (header, content) = GZIPInputStream(ByteArrayInputStream(this)).use { stream ->
            JsProtoBuf.Header.parseDelimitedFrom(stream, JsSerializerProtocol.extensionRegistry) to
            JsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        }

        return JsModuleDescriptor(
                name = name,
                data = KotlinJavaScriptLibraryParts(header, content.packageFragmentList),
                kind = when (content.kind) {
                    null, JsProtoBuf.Library.Kind.PLAIN -> ModuleKind.PLAIN
                    JsProtoBuf.Library.Kind.AMD -> ModuleKind.AMD
                    JsProtoBuf.Library.Kind.COMMON_JS -> ModuleKind.COMMON_JS
                    JsProtoBuf.Library.Kind.UMD -> ModuleKind.UMD
                },
                imported = content.importedModuleList
        )
    }
}

data class KotlinJavaScriptLibraryParts(val header: JsProtoBuf.Header, val body: List<ProtoBuf.PackageFragment>)

internal fun DeclarationDescriptor.extractFileId(): Int? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(JsProtoBuf.classContainingFileId)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(JsProtoBuf.functionContainingFileId)
    is DeserializedPropertyDescriptor -> proto.getExtension(JsProtoBuf.propertyContainingFileId)
    else -> null
}