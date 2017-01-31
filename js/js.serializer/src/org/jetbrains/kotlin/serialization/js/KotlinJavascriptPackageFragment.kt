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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class KotlinJavascriptPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        private val proto: ProtoBuf.PackageFragment,
        private val nameResolver: NameResolver
) : DeserializedPackageFragment(fqName, storageManager, module) {
    private val fileMap: Map<Int, FileHolder> by storageManager.createLazyValue {
        proto.getExtension(JsProtoBuf.packageFragmentFiles).fileList.withIndex().associate { (index, file) ->
            (if (file.hasId()) file.id else index) to FileHolder(file.annotationList)
        }
    }

    private val annotationDeserializer: AnnotationDeserializer by storageManager.createLazyValue {
        AnnotationDeserializer(module, components.notFoundClasses)
    }

    override val classDataFinder = KotlinJavascriptClassDataFinder(proto, nameResolver)

    override fun computeMemberScope(): DeserializedPackageMemberScope =
            DeserializedPackageMemberScope(
                    this, proto.`package`, nameResolver, containerSource = null, components = components,
                    classNames = { classDataFinder.allClassIds.filterNot(ClassId::isNestedClass).map { it.shortClassName } }
            )

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
        val annotations: List<AnnotationDescriptor> by storageManager.createLazyValue {
            annotationsProto.map { annotationDeserializer.deserializeAnnotation(it, nameResolver) }
        }
    }
}
