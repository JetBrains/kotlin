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

package org.jetbrains.kotlin.utils.serializer

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializationUtil
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializerExtension
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayOutputStream
import java.io.File

public class KotlinJavaScriptSerializer() {

    public fun serialize(moduleName: String, moduleDescriptor: ModuleDescriptor, metaFile: File) {
        FileUtil.writeToFile(metaFile, serializeToString(moduleName, moduleDescriptor))
    }

    public fun serializeToString(moduleName: String, moduleDescriptor: ModuleDescriptor): String {
        val contentMap = hashMapOf<String, ByteArray>()

        DescriptorUtils.getPackagesFqNames(moduleDescriptor).forEach {
            fqName ->
            serializePackage(moduleDescriptor, fqName) {
                fileName, stream ->
                contentMap[fileName] = stream.toByteArray()
            }
        }

        val content = KotlinJavascriptSerializationUtil.contentMapToByteArray(contentMap)
        return KotlinJavascriptMetadataUtils.writeMetadataToString(moduleName, content)
    }

    fun serializePackage(module: ModuleDescriptor, fqName: FqName, writeFun: (String, ByteArrayOutputStream) -> Unit) {
        val packageView = module.getPackage(fqName) ?: error("No package resolved in $module")

        val skip: (DeclarationDescriptor) -> Boolean = { DescriptorUtils.getContainingModule(it) != module}

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
}