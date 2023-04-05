/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlinx.metadata.*
import kotlinx.metadata.internal.*
import kotlinx.metadata.internal.common.*
import kotlinx.metadata.internal.extensions.*
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.StringTableImpl

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
internal class KlibMetadataExtensions : MetadataExtensions {

    private fun ReadContext.getSourceFile(index: Int) =
        strings.getString(index).let(::KlibSourceFile)

    private fun WriteContext.getIndexOf(file: KlibSourceFile) =
        strings.getStringIndex(file.name)

    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext) {
        val extension = v.visitExtensions(KlibClassExtensionVisitor.TYPE) as? KlibClassExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.classAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classUniqId)?.let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classFile)?.let {
            extension.visitFile(c.getSourceFile(it))
        }
        proto.enumEntryList.forEach { entryProto ->
            val ordinal = entryProto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryOrdinal)
            val name = c[entryProto.name]
            val uniqId = entryProto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryUniqId)?.readUniqId()
            val annotations = entryProto.getExtension(KlibMetadataProtoBuf.enumEntryAnnotation).map { it.readAnnotation(c.strings) }
            extension.visitEnumEntry(KlibEnumEntry(name, uniqId, ordinal, annotations.toMutableList()))
        }
    }

    override fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext) {
        val extension = v.visitExtensions(KlibPackageExtensionVisitor.TYPE) as? KlibPackageExtensionVisitor ?: return

        proto.getExtensionOrNull(KlibMetadataProtoBuf.packageFqName)?.let {
            val fqName = (c.strings as NameResolverImpl).getPackageFqName(it)
            extension.visitFqName(fqName)
        }
    }

    override fun readModuleFragmentExtensions(v: KmModuleFragmentVisitor, proto: ProtoBuf.PackageFragment, c: ReadContext) {
        val extension = v.visitExtensions(KlibModuleFragmentExtensionVisitor.TYPE) as? KlibModuleFragmentExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.packageFragmentFiles)
            .map { c.getSourceFile(it) }
            .forEach(extension::visitFile)
        proto.getExtensionOrNull(KlibMetadataProtoBuf.fqName)?.let(extension::visitFqName)
        proto.getExtension(KlibMetadataProtoBuf.className)
            .map(c.strings::getQualifiedClassName)
            .forEach(extension::visitClassName)
    }

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        val extension = v.visitExtensions(KlibFunctionExtensionVisitor.TYPE) as? KlibFunctionExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.functionAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.functionUniqId)?.let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.functionFile)?.let {
            val file = c.getSourceFile(it)
            extension.visitFile(file)
        }
    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext) {
        val extension = v.visitExtensions(KlibPropertyExtensionVisitor.TYPE) as? KlibPropertyExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.propertyAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtension(KlibMetadataProtoBuf.propertyGetterAnnotation).forEach { annotation ->
            extension.visitGetterAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtension(KlibMetadataProtoBuf.propertySetterAnnotation).forEach { annotation ->
            extension.visitSetterAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyUniqId)?.let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile)?.let(extension::visitFile)
        proto.getExtensionOrNull(KlibMetadataProtoBuf.compileTimeValue)?.let { value ->
            value.readAnnotationArgument(c.strings)?.let { extension.visitCompileTimeValue(it) }
        }
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext) {
        val extension = v.visitExtensions(KlibConstructorExtensionVisitor.TYPE) as? KlibConstructorExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.constructorAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.constructorUniqId)?.let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val extension = v.visitExtensions(KlibTypeParameterExtensionVisitor.TYPE) as? KlibTypeParameterExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.typeParameterAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.typeParamUniqId)?.let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext) {
        val extension = v.visitExtensions(KlibTypeExtensionVisitor.TYPE) as? KlibTypeExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.typeAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeAliasExtensions(v: KmTypeAliasVisitor, proto: ProtoBuf.TypeAlias, c: ReadContext) {
        val extension = v.visitExtensions(KlibTypeAliasExtensionVisitor.TYPE) as? KlibTypeAliasExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.typeAliasUniqId).let { descriptorUniqId ->
            extension.visitUniqId(descriptorUniqId.readUniqId())
        }
    }

    override fun readValueParameterExtensions(v: KmValueParameterVisitor, proto: ProtoBuf.ValueParameter, c: ReadContext) {
        val extension = v.visitExtensions(KlibValueParameterExtensionVisitor.TYPE) as? KlibValueParameterExtensionVisitor ?: return

        proto.getExtension(KlibMetadataProtoBuf.parameterAnnotation).forEach { annotation ->
            extension.visitAnnotation(annotation.readAnnotation(c.strings))
        }
    }

    override fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor? {
        if (type != KlibClassExtensionVisitor.TYPE) return null
        return object : KlibClassExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.classAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.classUniqId,
                    uniqId.writeUniqId().build()
                )
            }

            override fun visitFile(file: KlibSourceFile) {
                val fileIdx = c.getIndexOf(file)
                proto.setExtension(KlibMetadataProtoBuf.classFile, fileIdx)
            }

            override fun visitEnumEntry(entry: KlibEnumEntry) {
                val entryIndex = proto.enumEntryList.indexOfFirst { it.name == c[entry.name] }
                val entryAnnotationsProto = entry.annotations.map { it.writeAnnotation(c.strings).build() }
                val entryProto = ProtoBuf.EnumEntry.newBuilder()
                    .setName(c[entry.name])
                    .setExtension(KlibMetadataProtoBuf.enumEntryAnnotation, entryAnnotationsProto)
                entry.uniqId?.let { uniqId ->
                    entryProto.setExtension(KlibMetadataProtoBuf.enumEntryUniqId, uniqId.writeUniqId().build())
                }
                entry.ordinal?.let { ordinal ->
                    entryProto.setExtension(KlibMetadataProtoBuf.enumEntryOrdinal, ordinal)
                }
                if (entryIndex == -1) {
                    proto.addEnumEntry(entryProto.build())
                } else {
                    proto.setEnumEntry(entryIndex, entryProto.build())
                }
            }
        }
    }

    override fun writePackageExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Package.Builder,
        c: WriteContext
    ): KmPackageExtensionVisitor? {
        if (type != KlibPackageExtensionVisitor.TYPE) return null
        return object : KlibPackageExtensionVisitor() {
            override fun visitFqName(name: String) {
                val nameIdx = (c.strings as StringTableImpl).getPackageFqNameIndex(FqName(name))
                proto.setExtension(KlibMetadataProtoBuf.packageFqName, nameIdx)
            }
        }
    }

    override fun writeModuleFragmentExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.PackageFragment.Builder,
        c: WriteContext
    ): KmModuleFragmentExtensionVisitor? {
        if (type != KlibModuleFragmentExtensionVisitor.TYPE) return null
        return object : KlibModuleFragmentExtensionVisitor() {
            override fun visitFile(file: KlibSourceFile) {
                val fileIdx = c.getIndexOf(file)
                proto.addExtension(KlibMetadataProtoBuf.packageFragmentFiles, fileIdx)
            }

            override fun visitFqName(fqName: String) {
                proto.setExtension(KlibMetadataProtoBuf.fqName, fqName)
            }

            override fun visitClassName(className: ClassName) {
                val classNameIdx = (c.strings as StringTableImpl).getQualifiedClassNameIndex(ClassId.fromString(className))
                proto.addExtension(KlibMetadataProtoBuf.className, classNameIdx)
            }
        }
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Function.Builder,
        c: WriteContext
    ): KmFunctionExtensionVisitor? {
        if (type != KlibFunctionExtensionVisitor.TYPE) return null
        return object : KlibFunctionExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.functionAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.functionUniqId,
                    uniqId.writeUniqId().build()
                )
            }

            override fun visitFile(file: KlibSourceFile) {
                val index = c.getIndexOf(file)
                proto.setExtension(KlibMetadataProtoBuf.functionFile, index)
            }
        }
    }

    override fun writePropertyExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Property.Builder,
        c: WriteContext
    ): KmPropertyExtensionVisitor? {
        if (type != KlibPropertyExtensionVisitor.TYPE) return null
        return object : KlibPropertyExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.propertyAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitGetterAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.propertyGetterAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitSetterAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.propertySetterAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.propertyUniqId,
                    uniqId.writeUniqId().build()
                )
            }

            override fun visitFile(file: Int) {
                proto.setExtension(
                    KlibMetadataProtoBuf.propertyFile,
                    file
                )
            }

            override fun visitCompileTimeValue(value: KmAnnotationArgument) {
                proto.setExtension(
                    KlibMetadataProtoBuf.compileTimeValue,
                    value.writeAnnotationArgument(c.strings).build()
                )
            }
        }
    }

    override fun writeConstructorExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.Constructor.Builder,
        c: WriteContext
    ): KmConstructorExtensionVisitor? {
        if (type != KlibConstructorExtensionVisitor.TYPE) return null
        return object : KlibConstructorExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.constructorAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.constructorUniqId,
                    uniqId.writeUniqId().build()
                )
            }
        }
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.TypeParameter.Builder,
        c: WriteContext
    ): KmTypeParameterExtensionVisitor? {
        if (type != KlibTypeParameterExtensionVisitor.TYPE) return null
        return object : KlibTypeParameterExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.typeParameterAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }

            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.typeParamUniqId,
                    uniqId.writeUniqId().build()
                )
            }
        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        if (type != KlibTypeExtensionVisitor.TYPE) return null
        return object : KlibTypeExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.typeAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }
        }
    }

    override fun writeTypeAliasExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.TypeAlias.Builder,
        c: WriteContext
    ): KmTypeAliasExtensionVisitor? {
        if (type != KlibTypeAliasExtensionVisitor.TYPE) return null
        return object : KlibTypeAliasExtensionVisitor() {
            override fun visitUniqId(uniqId: UniqId) {
                proto.setExtension(
                    KlibMetadataProtoBuf.typeAliasUniqId,
                    uniqId.writeUniqId().build()
                )
            }
        }
    }

    override fun writeValueParameterExtensions(
        type: KmExtensionType,
        proto: ProtoBuf.ValueParameter.Builder,
        c: WriteContext
    ): KmValueParameterExtensionVisitor? {
        if (type != KlibValueParameterExtensionVisitor.TYPE) return null
        return object : KlibValueParameterExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(
                    KlibMetadataProtoBuf.parameterAnnotation,
                    annotation.writeAnnotation(c.strings).build()
                )
            }
        }
    }

    override fun createClassExtension(): KmClassExtension =
        KlibClassExtension()

    override fun createPackageExtension(): KmPackageExtension =
        KlibPackageExtension()

    override fun createModuleFragmentExtensions(): KmModuleFragmentExtension =
        KlibModuleFragmentExtension()

    override fun createFunctionExtension(): KmFunctionExtension =
        KlibFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension =
        KlibPropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension =
        KlibConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension =
        KlibTypeParameterExtension()

    override fun createTypeExtension(): KmTypeExtension =
        KlibTypeExtension()

    override fun createTypeAliasExtension(): KmTypeAliasExtension =
        KlibTypeAliasExtension()

    override fun createValueParameterExtension(): KmValueParameterExtension =
        KlibValueParameterExtension()
}
