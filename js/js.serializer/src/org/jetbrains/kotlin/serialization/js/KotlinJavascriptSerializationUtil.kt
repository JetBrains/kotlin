/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.JsMetadataVersion
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

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
