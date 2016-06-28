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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

class KotlinJavascriptPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        private val loadResource: (path: String) -> InputStream?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    private val nameResolver =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(fqName)).use { stream ->
                NameResolverImpl.read(stream)
            }

    private val fileMap: Map<Int, FileHolder> by lazy {
        loadResource(KotlinJavascriptSerializedResourcePaths.getFileListFilePath(fqName))?.use { rawInput ->
            val input = CodedInputStream.newInstance(rawInput)
            val count = input.readInt32()
            val result = mutableListOf<JsProtoBuf.File>()
            (1..count).forEach { result += JsProtoBuf.File.parseFrom(input) }
            result.map { it.id to FileHolder(it.annotationList) }.toMap()
        }.orEmpty()
    }

    private val annotationDeserializer: AnnotationDeserializer by lazy {
        AnnotationDeserializer(module, components.notFoundClasses)
    }

    override val classDataFinder = KotlinJavascriptClassDataFinder(nameResolver, loadResource)

    override fun computeMemberScope(): DeserializedPackageMemberScope =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getPackageFilePath(fqName)).use { packageStream ->
                val packageProto = ProtoBuf.Package.parseFrom(packageStream, JsSerializerProtocol.extensionRegistry)
                DeserializedPackageMemberScope(
                        this, packageProto, nameResolver, containerSource = null, components = components,
                        classNames = { loadClassNames() }
                )
            }

    private fun loadClassNames(): Collection<Name> =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(fqName)).use { classesStream ->
                val classesProto = JsProtoBuf.Classes.parseFrom(classesStream, JsSerializerProtocol.extensionRegistry)
                classesProto.classNameList?.map { id -> nameResolver.getName(id) } ?: listOf()
            }

    private fun loadResourceSure(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")

    fun getContainingFileAnnotations(descriptor: DeclarationDescriptor): List<AnnotationDescriptor> {
        if (DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java) != this) {
            throw IllegalArgumentException("Provided descriptor $descriptor does not belong to this package $this")
        }
        val fileId = when (descriptor) {
            is DeserializedClassDescriptor -> descriptor.classProto.getExtension(JsProtoBuf.classContainingFileId)
            is DeserializedSimpleFunctionDescriptor -> descriptor.proto.getExtension(JsProtoBuf.functionContainingFileId)
            is DeserializedPropertyDescriptor -> descriptor.proto.getExtension(JsProtoBuf.propertyContainingFileId)
            else -> null
        }

        return fileId?.let { fileMap[it] }?.annotations.orEmpty()
    }

    private inner class FileHolder(val annotationsProto: List<ProtoBuf.Annotation>) {
        val annotations: List<AnnotationDescriptor> by lazy {
            annotationsProto.map { annotationDeserializer.deserializeAnnotation(it, nameResolver) }
        }
    }
}
