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

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragmentImpl
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class KotlinJavascriptPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        proto: ProtoBuf.PackageFragment,
        header: JsProtoBuf.Header
) : DeserializedPackageFragmentImpl(fqName, storageManager, module, proto, JsContainerSource(fqName, header)) {
    private val fileMap: Map<Int, FileHolder> by storageManager.createLazyValue {
        this.proto.getExtension(JsProtoBuf.packageFragmentFiles).fileList.withIndex().associate { (index, file) ->
            (if (file.hasId()) file.id else index) to FileHolder(file.annotationList)
        }
    }

    private val annotationDeserializer: AnnotationDeserializer by storageManager.createLazyValue {
        AnnotationDeserializer(module, components.notFoundClasses)
    }

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

    private class JsContainerSource(private val fqName: FqName, private val header: JsProtoBuf.Header) : DeserializedContainerSource {
        // TODO
        override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

        // This is null because we look for incompatible libraries in dependencies in the beginning of the compilation anyway,
        // and refuse to compile against them completely
        override val incompatibility: IncompatibleVersionErrorData<*>?
            get() = null

        override val isPreReleaseInvisible: Boolean
            get() = (header.flags and 1) != 0 && !KotlinCompilerVersion.isPreRelease()

        // TODO: this is not a class
        override val presentableString: String
            get() = "Package '$fqName'"
    }
}
