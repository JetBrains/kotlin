/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.FlexibleType

class KotlinJavascriptSerializerExtension(
        private val fileRegistry: KotlinFileRegistry,
        packageFqName: FqName
) : KotlinSerializerExtensionBase(JsSerializerProtocol, packageFqName) {
    override val stringTable = JavaScriptStringTable()

    override fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(DynamicTypeDeserializer.id)
    }

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsProtoBuf.classContainingFileId, id)
        }
        super.serializeClass(descriptor, proto)
    }

    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsProtoBuf.propertyContainingFileId, id)
        }
        super.serializeProperty(descriptor, proto)
    }

    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) {
        val id = getFileId(descriptor)
        if (id != null) {
            proto.setExtension(JsProtoBuf.functionContainingFileId, id)
        }
        super.serializeFunction(descriptor, proto)
    }

    private fun getFileId(descriptor: DeclarationDescriptor): Int? {
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor) || descriptor !is DeclarationDescriptorWithSource) return null

        val file = descriptor.source.containingFile
        if (file !is PsiSourceFile) return null

        val psiFile = file.psiFile
        return (psiFile as? KtFile)?.let { fileRegistry.lookup(it) }
    }
}

object JsSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply { JsProtoBuf.registerAllExtensions(this) },
        JsProtoBuf.packageFqName,
        JsProtoBuf.constructorAnnotation, JsProtoBuf.classAnnotation, JsProtoBuf.functionAnnotation, JsProtoBuf.propertyAnnotation,
        JsProtoBuf.enumEntryAnnotation, JsProtoBuf.compileTimeValue, JsProtoBuf.parameterAnnotation, JsProtoBuf.typeAnnotation,
        JsProtoBuf.typeParameterAnnotation
) {
    fun getKjsmFilePath(packageFqName: FqName): String {
        val shortName = if (packageFqName.isRoot) Name.identifier("root-package") else packageFqName.shortName()

        return packageFqName.child(shortName).asString().replace('.', '/') +
               "." +
               KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION
    }
}
