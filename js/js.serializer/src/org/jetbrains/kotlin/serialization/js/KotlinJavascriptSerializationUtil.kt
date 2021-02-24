/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.filterOutSourceAnnotations
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
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
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KotlinJavascriptSerializationUtil {
    const val CLASS_METADATA_FILE_EXTENSION: String = "kjsm"

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
        return createKotlinJavascriptPackageFragmentProvider(
            storageManager, module, headerProto, scopeProto, metadata.metadataVersion, configuration, lookupTracker
        )
    }

    fun serializeMetadata(
        bindingContext: BindingContext,
        jsDescriptor: JsModuleDescriptor<ModuleDescriptor>,
        languageVersionSettings: LanguageVersionSettings,
        metadataVersion: JsMetadataVersion,
        project: Project
    ): SerializedMetadata {
        val serializedFragments =
            emptyMap<FqName, ByteArray>().missingMetadata(bindingContext, jsDescriptor.data, languageVersionSettings, metadataVersion, project)

        return SerializedMetadata(serializedFragments, jsDescriptor, languageVersionSettings, metadataVersion)
    }

    class SerializedMetadata(
        private val serializedFragments: Map<FqName, ByteArray>,
        private val jsDescriptor: JsModuleDescriptor<ModuleDescriptor>,
        private val languageVersionSettings: LanguageVersionSettings,
        private val metadataVersion: JsMetadataVersion
    ) {
        class SerializedPackage(val fqName: FqName, val bytes: ByteArray)

        fun serializedPackages(): List<SerializedPackage> {
            val packages = arrayListOf<SerializedPackage>()

            for ((fqName, part) in serializedFragments) {
                val stream = ByteArrayOutputStream()
                with(DataOutputStream(stream)) {
                    val version = metadataVersion.toArray()
                    writeInt(version.size)
                    version.forEach(this::writeInt)
                }

                serializeHeader(jsDescriptor.data, fqName, languageVersionSettings).writeDelimitedTo(stream)
                stream.write(part)

                packages.add(SerializedPackage(fqName, stream.toByteArray()))
            }

            return packages
        }

        fun asString(): String =
            KotlinJavascriptMetadataUtils.formatMetadataAsString(jsDescriptor.name, asByteArray(), metadataVersion)

        private fun asByteArray(): ByteArray =
            ByteArrayOutputStream().apply {
                GZIPOutputStream(this).use { stream ->
                    serializeHeader(
                        jsDescriptor.data,
                        packageFqName = null,
                        languageVersionSettings = languageVersionSettings
                    ).writeDelimitedTo(stream)
                    asLibrary().writeTo(stream)
                    stream.appendPackageFragments()
                    jsDescriptor.imported.forEach {
                        stream.writeProto(JsProtoBuf.Library.IMPORTED_MODULE_FIELD_NUMBER, it.toByteArray())
                    }
                }
            }.toByteArray()

        private fun asLibrary(): JsProtoBuf.Library {
            val moduleKind = jsDescriptor.kind
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

            return builder.build()
        }

        private fun OutputStream.writeProto(fieldNumber: Int, content: ByteArray) {
            // Message header
            write((fieldNumber shl 3) or 2)
            // Size varint
            var size = content.size
            while (size > 0x7F) {
                write(0x80 or (size and 0x7F))
                size = size ushr 7
            }
            write(size)
            // Fragment itself
            write(content)
        }

        private fun OutputStream.appendPackageFragments() {
            for ((_, fragment) in serializedFragments.entries.sortedBy { (fqName, _) -> fqName.asString() }) {
                writeProto(JsProtoBuf.Library.PACKAGE_FRAGMENT_FIELD_NUMBER, fragment)
            }
        }
    }

    fun serializeDescriptors(
        bindingContext: BindingContext,
        module: ModuleDescriptor,
        scope: Collection<DeclarationDescriptor>,
        fqName: FqName,
        languageVersionSettings: LanguageVersionSettings,
        project: Project,
        metadataVersion: BinaryVersion
    ): ProtoBuf.PackageFragment {
        val builder = ProtoBuf.PackageFragment.newBuilder()

        val skip = fun(descriptor: DeclarationDescriptor): Boolean {
            // TODO: ModuleDescriptor should be able to return the package only with the contents of that module, without dependencies
            if (descriptor.module != module) return true

            if (descriptor is MemberDescriptor && descriptor.isExpect) {
                return !(descriptor is ClassDescriptor && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(descriptor))
            }

            return false
        }

        val fileRegistry = KotlinFileRegistry()
        val extension = KotlinJavascriptSerializerExtension(fileRegistry, languageVersionSettings, metadataVersion)

        val classDescriptors = scope.filterIsInstance<ClassDescriptor>().sortedBy { it.fqNameSafe.asString() }

        fun serializeClasses(descriptors: Collection<DeclarationDescriptor>, parentSerializer: DescriptorSerializer) {
            for (descriptor in descriptors) {
                if (descriptor !is ClassDescriptor || skip(descriptor)) continue

                val serializer = DescriptorSerializer.create(descriptor, extension, parentSerializer, project)
                serializeClasses(descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors(), serializer)
                val classProto = serializer.classProto(descriptor).build() ?: error("Class not serialized: $descriptor")
                builder.addClass_(classProto)
            }
        }

        val serializer = DescriptorSerializer.createTopLevel(extension)
        serializeClasses(classDescriptors, serializer)

        val stringTable = extension.stringTable

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
            for (annotation in annotations.filterOutSourceAnnotations()) {
                fileProto.addAnnotation(serializer.serializeAnnotation(annotation))
            }
            filesProto.addFile(fileProto)
        }
        return filesProto.build()
    }

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

        val experimentalAnnotationFqNames = languageVersionSettings.getFlag(AnalysisFlags.experimental)
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

    fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
        return mutableSetOf<FqName>().apply {
            getSubPackagesFqNames(module.packageFragmentProviderForModuleContentWithoutDependencies, FqName.ROOT, this)
            add(FqName.ROOT)
        }
    }

    private fun getSubPackagesFqNames(packageFragmentProvider: PackageFragmentProvider, fqName: FqName, result: MutableSet<FqName>) {
        if (!fqName.isRoot) {
            result.add(fqName)
        }

        for (subPackage in packageFragmentProvider.getSubPackagesOf(fqName) { true }) {
            getSubPackagesFqNames(packageFragmentProvider, subPackage, result)
        }
    }

    @JvmStatic
    fun readModuleAsProto(metadata: ByteArray, metadataVersion: JsMetadataVersion): KotlinJavaScriptLibraryParts {
        val (header, content) = GZIPInputStream(ByteArrayInputStream(metadata)).use { stream ->
            JsProtoBuf.Header.parseDelimitedFrom(stream, JsSerializerProtocol.extensionRegistry) to
                    JsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        }

        val moduleKind = when (content.kind) {
            null, JsProtoBuf.Library.Kind.PLAIN -> ModuleKind.PLAIN
            JsProtoBuf.Library.Kind.AMD -> ModuleKind.AMD
            JsProtoBuf.Library.Kind.COMMON_JS -> ModuleKind.COMMON_JS
            JsProtoBuf.Library.Kind.UMD -> ModuleKind.UMD
        }

        return KotlinJavaScriptLibraryParts(header, content.packageFragmentList, moduleKind, content.importedModuleList, metadataVersion)
    }
}

fun Map<FqName, ByteArray>.missingMetadata(
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor,
    languageVersionSettings: LanguageVersionSettings,
    metadataVersion: JsMetadataVersion,
    project: Project
): Map<FqName, ByteArray> {
    val serializedFragments = HashMap<FqName, ByteArray>()

    for (fqName in KotlinJavascriptSerializationUtil.getPackagesFqNames(moduleDescriptor).sortedBy { it.asString() }) {
        if (fqName in this) continue

        val fragment = KotlinJavascriptSerializationUtil.serializeDescriptors(
            bindingContext, moduleDescriptor,
            moduleDescriptor.packageFragmentProviderForModuleContentWithoutDependencies.packageFragments(fqName).flatMap {
                it.getMemberScope().getContributedDescriptors()
            },
            fqName, languageVersionSettings, project, metadataVersion
        )

        if (!fragment.isEmpty()) {
            serializedFragments[fqName] = fragment.toByteArray()
        }
    }

    return serializedFragments
}

private val ModuleDescriptor.packageFragmentProviderForModuleContentWithoutDependencies
    get() = (this as? ModuleDescriptorImpl)?.packageFragmentProviderForModuleContentWithoutDependencies
        ?: throw IllegalStateException("Unsupported ModuleDescriptor kind: ${this::javaClass}")

private fun ProtoBuf.PackageFragment.isEmpty(): Boolean =
    class_Count == 0 && `package`.let { it.functionCount == 0 && it.propertyCount == 0 && it.typeAliasCount == 0 }

data class KotlinJavaScriptLibraryParts(
    val header: JsProtoBuf.Header,
    val body: List<ProtoBuf.PackageFragment>,
    val kind: ModuleKind,
    val importedModules: List<String>,
    val metadataVersion: JsMetadataVersion
)

internal fun DeclarationDescriptor.extractFileId(): Int? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(JsProtoBuf.classContainingFileId)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(JsProtoBuf.functionContainingFileId)
    is DeserializedPropertyDescriptor -> proto.getExtension(JsProtoBuf.propertyContainingFileId)
    else -> null
}
