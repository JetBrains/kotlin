/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragment
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

private fun sourceByIndex(descriptor: CallableMemberDescriptor, index: Int): SourceFile {
    val fragment = descriptor.findPackage() as KlibMetadataDeserializedPackageFragment
    val fileName = fragment.proto.strings.stringList[index]
    val library = (descriptor.module.klibModuleOrigin as DeserializedKlibModuleOrigin).library
    return DeserializedSourceFile(fileName, library)
}

@OptIn(K1Deprecation::class)
internal fun CallableMemberDescriptor.findSourceFile(): SourceFile {
    val source = this.source.containingFile
    if (source != SourceFile.NO_SOURCE_FILE)
        return source
    return when {
        this is DeserializedSimpleFunctionDescriptor && proto.hasExtension(KlibMetadataProtoBuf.functionFile) ->
            sourceByIndex(
                this, proto.getExtension(KlibMetadataProtoBuf.functionFile))
        this is DeserializedPropertyDescriptor && proto.hasExtension(KlibMetadataProtoBuf.propertyFile) ->
            sourceByIndex(
                this, proto.getExtension(KlibMetadataProtoBuf.propertyFile))
        else -> TODO()
    }
}

@OptIn(K1Deprecation::class)
internal fun DeclarationDescriptor.extractSerializedKdocString(): String? = when (this) {
    is DeserializedClassDescriptor -> classProto.getExtension(KlibMetadataProtoBuf.classKdoc)
    is DeserializedSimpleFunctionDescriptor -> proto.getExtension(KlibMetadataProtoBuf.functionKdoc)
    is DeserializedPropertyDescriptor -> proto.getExtension(KlibMetadataProtoBuf.propertyKdoc)
    is DeserializedClassConstructorDescriptor -> proto.getExtension(KlibMetadataProtoBuf.constructorKdoc)
    else -> null
}
