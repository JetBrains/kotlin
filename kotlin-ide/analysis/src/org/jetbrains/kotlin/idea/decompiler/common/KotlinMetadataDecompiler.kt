/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.common

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.IOException

abstract class KotlinMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : ClassFileDecompilers.Full() {
    private val metadataStubBuilder: KotlinMetadataStubBuilder =
        KotlinMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)

    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    abstract fun readFile(bytes: ByteArray, file: VirtualFile): FileWithMetadata?

    override fun accepts(file: VirtualFile) = file.fileType == fileType

    override fun getStubBuilder() = metadataStubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            if (readFileSafely(provider.virtualFile) == null) {
                null
            } else {
                KtDecompiledFile(provider, this::buildDecompiledText)
            }
        }
    }

    private fun readFileSafely(file: VirtualFile, content: ByteArray? = null): FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            readFile(content ?: file.contentsToByteArray(false), file)
        } catch (e: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }

    private fun buildDecompiledText(virtualFile: VirtualFile): DecompiledText {
        if (virtualFile.fileType != fileType) {
            error("Unexpected file type ${virtualFile.fileType}")
        }

        val file = readFileSafely(virtualFile)

        return when (file) {
            null -> {
                createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), invalidBinaryVersion())
            }
            is FileWithMetadata.Incompatible -> {
                createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), file.version)
            }
            is FileWithMetadata.Compatible -> {
                val packageFqName = file.packageFqName
                val resolver = KotlinMetadataDeserializerForDecompiler(
                    packageFqName, file.proto, file.nameResolver, file.version,
                    serializerProtocol(), flexibleTypeDeserializer
                )
                val declarations = arrayListOf<DeclarationDescriptor>()
                declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
                for (classProto in file.classesToDecompile) {
                    val classId = file.nameResolver.getClassId(classProto.fqName)
                    declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
                }
                buildDecompiledText(packageFqName, declarations, renderer)
            }
        }
    }
}

sealed class FileWithMetadata {
    class Incompatible(val version: BinaryVersion) : FileWithMetadata()

    open class Compatible(
        val proto: ProtoBuf.PackageFragment,
        val version: BinaryVersion,
        serializerProtocol: SerializerExtensionProtocol
    ) : FileWithMetadata() {
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
        val packageFqName = FqName(nameResolver.getPackageFqName(proto.`package`.getExtension(serializerProtocol.packageFqName)))

        open val classesToDecompile: List<ProtoBuf.Class> =
            proto.class_List.filter { proto ->
                val classId = nameResolver.getClassId(proto.fqName)
                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
            }
    }
}
