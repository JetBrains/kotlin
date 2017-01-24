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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KotlinJavascriptSerializationUtil {
    val CLASS_METADATA_FILE_EXTENSION: String = "kjsm"

    @JvmStatic
    fun readModule(
            metadata: ByteArray, storageManager: StorageManager, module: ModuleDescriptor, configuration: DeserializationConfiguration
    ): JsModuleDescriptor<PackageFragmentProvider?> {
        val jsModule = metadata.deserializeToLibraryParts(module.name.asString())
        return jsModule.copy(createKotlinJavascriptPackageFragmentProvider(storageManager, module, jsModule.data, configuration))
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
            val part = JsProtoBuf.Library.Part.newBuilder()
            serializePackage(bindingContext, module, fqName, object : SerializerCallbacks {
                override fun writeClass(classId: ClassId, classProto: ProtoBuf.Class) {
                    part.addClass_(classProto)
                }

                override fun writePackage(fqName: FqName, packageProto: ProtoBuf.Package) {
                    part.`package` = packageProto
                }

                override fun writeFiles(fqName: FqName, filesProto: JsProtoBuf.Files) {
                    part.files = filesProto
                }

                override fun writeStringTable(fqName: FqName, stringTable: StringTableImpl) {
                    val (strings, qualifiedNames) = stringTable.buildProto()
                    part.strings = strings
                    part.qualifiedNames = qualifiedNames
                }

                override fun writeClassNames(fqName: FqName, classNames: List<Name>, stringTable: StringTableImpl) {
                    // Do nothing
                }
            })

            if (part.hasPackage() || part.class_Count > 0) {
                builder.addPart(part)
            }
        }

        return builder.build()
    }

    fun metadataAsString(bindingContext: BindingContext, jsDescriptor: JsModuleDescriptor<ModuleDescriptor>): String =
            KotlinJavascriptMetadataUtils.formatMetadataAsString(jsDescriptor.name, jsDescriptor.serializeToBinaryMetadata(bindingContext))

    interface SerializerCallbacks {
        fun writeClass(classId: ClassId, classProto: ProtoBuf.Class)

        fun writePackage(fqName: FqName, packageProto: ProtoBuf.Package)

        fun writeFiles(fqName: FqName, filesProto: JsProtoBuf.Files)

        fun writeStringTable(fqName: FqName, stringTable: StringTableImpl)

        fun writeClassNames(fqName: FqName, classNames: List<Name>, stringTable: StringTableImpl)
    }

    fun serializePackage(bindingContext: BindingContext, module: ModuleDescriptor, fqName: FqName, callbacks: SerializerCallbacks) {
        val packageView = module.getPackage(fqName)

        // TODO: ModuleDescriptor should be able to return the package only with the contents of that module, without dependencies
        val skip: (DeclarationDescriptor) -> Boolean = { DescriptorUtils.getContainingModule(it) != module || (it is MemberDescriptor && it.isHeader) }

        val fileRegistry = KotlinFileRegistry()
        val serializerExtension = KotlinJavascriptSerializerExtension(fileRegistry, fqName)
        val serializer = DescriptorSerializer.createTopLevel(serializerExtension)

        val classifierDescriptors = DescriptorSerializer.sort(packageView.memberScope.getContributedDescriptors(
                DescriptorKindFilter.CLASSIFIERS))

        ClassSerializationUtil.serializeClasses(classifierDescriptors, serializer, object : ClassSerializationUtil.Sink {
            override fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class) {
                callbacks.writeClass(classDescriptor.classId!!, classProto)
            }
        }, skip)

        val stringTable = serializerExtension.stringTable

        val fragments = packageView.fragments
        val members = fragments
                .flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) }
                .filterNot(skip)
        val packageProto = serializer.packagePartProto(members)
        packageProto.setExtension(JsProtoBuf.packageFqName, stringTable.getPackageFqNameIndex(fqName))
        callbacks.writePackage(fqName, packageProto.build())

        val fileTable = serializeFiles(fileRegistry, bindingContext, AnnotationSerializer(stringTable))
        callbacks.writeFiles(fqName, fileTable)

        val classNames = DescriptorSerializer.sort(fragments.flatMap { fragment ->
            fragment.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassDescriptor>()
        }.filterNot(skip)).map { it.name }
        callbacks.writeClassNames(fqName, classNames, stringTable)

        callbacks.writeStringTable(fqName, stringTable)
    }

    private fun serializeFiles(
            fileRegistry: KotlinFileRegistry,
            bindingContext: BindingContext,
            serializer: AnnotationSerializer
    ): JsProtoBuf.Files {
        val filesProto = JsProtoBuf.Files.newBuilder()
        for ((file, id) in fileRegistry.fileIds) {
            val fileProto = JsProtoBuf.File.newBuilder()
            fileProto.id = id
            for (annotationPsi in file.annotationEntries) {
                val annotation = bindingContext[BindingContext.ANNOTATION, annotationPsi]!!
                fileProto.addAnnotation(serializer.serializeAnnotation(annotation))
            }
            filesProto.addFile(fileProto)
        }
        return filesProto.build()
    }

    fun toContentMap(bindingContext: BindingContext, module: ModuleDescriptor): Map<String, ByteArray> {
        val contentMap = hashMapOf<String, ByteArray>()

        fun writeFile(fileName: String, bytes: ByteArray) {
            contentMap[fileName] = bytes
        }

        for (fqName in getPackagesFqNames(module)) {
            serializePackage(bindingContext, module, fqName, object : SerializerCallbacks {
                override fun writeClass(classId: ClassId, classProto: ProtoBuf.Class) {
                    val bytes = ByteArrayOutputStream().apply(classProto::writeTo).toByteArray()
                    writeFile(KotlinJavascriptSerializedResourcePaths.getClassMetadataPath(classId), bytes)
                }

                override fun writePackage(fqName: FqName, packageProto: ProtoBuf.Package) {
                    if (packageProto.functionCount > 0 || packageProto.propertyCount > 0 || packageProto.typeAliasCount > 0) {
                        val bytes = ByteArrayOutputStream().apply(packageProto::writeTo).toByteArray()
                        writeFile(KotlinJavascriptSerializedResourcePaths.getPackageFilePath(fqName), bytes)
                    }
                }

                override fun writeFiles(fqName: FqName, filesProto: JsProtoBuf.Files) {
                    val bytes = ByteArrayOutputStream().apply(filesProto::writeTo).toByteArray()
                    writeFile(KotlinJavascriptSerializedResourcePaths.getFileListFilePath(fqName), bytes)
                }

                override fun writeStringTable(fqName: FqName, stringTable: StringTableImpl) {
                    val bytes = ByteArrayOutputStream().apply(stringTable::serializeTo).toByteArray()
                    if (bytes.isNotEmpty()) {
                        writeFile(KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(fqName), bytes)
                    }
                }

                override fun writeClassNames(fqName: FqName, classNames: List<Name>, stringTable: StringTableImpl) {
                    val builder = JsProtoBuf.Classes.newBuilder()
                    builder.addAllClassName(classNames.map(stringTable::getSimpleNameIndex))
                    val classesProto = builder.build()
                    if (classesProto.classNameCount > 0) {
                        val bytes = ByteArrayOutputStream().apply(classesProto::writeTo).toByteArray()
                        writeFile(KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(fqName), bytes)
                    }
                }
            })
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

    private fun JsModuleDescriptor<ModuleDescriptor>.serializeToBinaryMetadata(bindingContext: BindingContext): ByteArray {
        val proto = serializeMetadata(bindingContext, data, kind, imported)
        return ByteArrayOutputStream().apply {
            GZIPOutputStream(this).use(proto::writeTo)
        }.toByteArray()
    }

    private fun ByteArray.deserializeToLibraryParts(name: String): JsModuleDescriptor<List<JsProtoBuf.Library.Part>> {
        val content = GZIPInputStream(ByteArrayInputStream(this)).use { stream ->
            JsProtoBuf.Library.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        }

        return JsModuleDescriptor(
                name = name,
                data = content.partList,
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
