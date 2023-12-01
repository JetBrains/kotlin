/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.StringTableImpl
import kotlin.metadata.*
import kotlin.metadata.internal.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.*

internal class KlibMetadataExtensions : MetadataExtensions {

    private fun ReadContext.getSourceFile(index: Int) =
        strings.getString(index).let(::KlibSourceFile)

    private fun WriteContext.getIndexOf(file: KlibSourceFile) =
        strings.getStringIndex(file.name)

    override fun readClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class, c: ReadContext) {
        val extension = kmClass.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.classAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classFile)?.let {
            extension.file = c.getSourceFile(it)
        }
        for (entryProto in proto.enumEntryList) {
            val ordinal = entryProto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryOrdinal)
            val name = c[entryProto.name]
            val uniqId = entryProto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryUniqId)?.readUniqId()
            val annotations = entryProto.getExtension(KlibMetadataProtoBuf.enumEntryAnnotation).map { it.readAnnotation(c.strings) }
            extension.enumEntries.add(KlibEnumEntry(name, uniqId, ordinal, annotations.toMutableList()))
        }
    }

    override fun readPackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package, c: ReadContext) {
        val extension = kmPackage.klibExtensions

        proto.getExtensionOrNull(KlibMetadataProtoBuf.packageFqName)?.let {
            extension.fqName = (c.strings as NameResolverImpl).getPackageFqName(it)
        }
    }

    override fun readModuleFragmentExtensions(kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment, c: ReadContext) {
        val extension = kmModuleFragment.klibExtensions

        for (file in proto.getExtension(KlibMetadataProtoBuf.packageFragmentFiles)) {
            extension.moduleFragmentFiles.add(c.getSourceFile(file))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.fqName)?.let {
            extension.fqName = it
        }
        for (className in proto.getExtension(KlibMetadataProtoBuf.className)) {
            extension.className.add(c.strings.getQualifiedClassName(className))
        }
    }

    override fun readFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function, c: ReadContext) {
        val extension = kmFunction.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.functionAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.functionUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.functionFile)?.let {
            extension.file = c.getSourceFile(it)
        }
    }

    override fun readPropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property, c: ReadContext) {
        val extension = kmProperty.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.propertyAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
        for (annotation in proto.getExtension(KlibMetadataProtoBuf.propertyGetterAnnotation)) {
            extension.getterAnnotations.add(annotation.readAnnotation(c.strings))
        }
        for (annotation in proto.getExtension(KlibMetadataProtoBuf.propertySetterAnnotation)) {
            extension.setterAnnotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile)?.let { file ->
            extension.file = file
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.compileTimeValue)?.let { value ->
            value.readAnnotationArgument(c.strings)?.let {
                extension.compileTimeValue = it
            }
        }
    }

    override fun readConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor, c: ReadContext) {
        val extension = kmConstructor.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.constructorAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.constructorUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
    }

    override fun readTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val extension = kmTypeParameter.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.typeParameterAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.typeParamUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
    }

    override fun readTypeExtensions(kmType: KmType, proto: ProtoBuf.Type, c: ReadContext) {
        val extension = kmType.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.typeAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeAliasExtensions(kmTypeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext) {
        val extension = kmTypeAlias.klibExtensions

        proto.getExtension(KlibMetadataProtoBuf.typeAliasUniqId).let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
    }

    override fun readValueParameterExtensions(kmValueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter, c: ReadContext) {
        val extension = kmValueParameter.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.parameterAnnotation)) {
            extension.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun writeClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class.Builder, c: WriteContext) {
        for (annotation in kmClass.klibAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.classAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        for (entry in kmClass.klibEnumEntries) {
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

        kmClass.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.classUniqId, uniqId.writeUniqId().build())
        }

        kmClass.file?.let { file ->
            proto.setExtension(KlibMetadataProtoBuf.classFile, c.getIndexOf(file))
        }
    }

    override fun writePackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext) {
        kmPackage.fqName?.let { fqName ->
            proto.setExtension(KlibMetadataProtoBuf.packageFqName, c.strings.getPackageFqNameIndexByString(fqName))
        }
    }

    override fun writeModuleFragmentExtensions(
        kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext,
    ) {
        for (file in kmModuleFragment.moduleFragmentFiles) {
            proto.addExtension(KlibMetadataProtoBuf.packageFragmentFiles, c.getIndexOf(file))
        }

        kmModuleFragment.fqName?.let { fqName ->
            proto.setExtension(KlibMetadataProtoBuf.fqName, fqName)
        }

        for (className in kmModuleFragment.className) {
            val classNameIdx = (c.strings as StringTableImpl).getQualifiedClassNameIndex(ClassId.fromString(className))
            proto.addExtension(KlibMetadataProtoBuf.className, classNameIdx)
        }
    }

    override fun writeFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext) {
        for (annotation in kmFunction.klibAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.functionAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        kmFunction.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.functionUniqId, uniqId.writeUniqId().build())
        }

        kmFunction.file?.let { file ->
            proto.setExtension(KlibMetadataProtoBuf.functionFile, c.getIndexOf(file))
        }
    }

    override fun writePropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext) {
        for (annotation in kmProperty.klibAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.propertyAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.klibGetterAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.propertyGetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.klibSetterAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.propertySetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        kmProperty.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.propertyUniqId, uniqId.writeUniqId().build())
        }

        kmProperty.file?.let { file ->
            proto.setExtension(KlibMetadataProtoBuf.propertyFile, file)
        }

        kmProperty.compileTimeValue?.let { value ->
            proto.setExtension(KlibMetadataProtoBuf.compileTimeValue, value.writeAnnotationArgument(c.strings).build())
        }
    }

    override fun writeConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext) {
        for (annotation in kmConstructor.klibAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.constructorAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        kmConstructor.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.constructorUniqId, uniqId.writeUniqId().build())
        }
    }

    override fun writeTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext) {
        for (annotation in kmTypeParameter.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        kmTypeParameter.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.typeParamUniqId, uniqId.writeUniqId().build())
        }
    }

    override fun writeTypeExtensions(type: KmType, proto: ProtoBuf.Type.Builder, c: WriteContext) {
        for (annotation in type.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.typeAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeTypeAliasExtensions(typeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext) {
        typeAlias.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.typeAliasUniqId, uniqId.writeUniqId().build())
        }
    }

    override fun writeValueParameterExtensions(valueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext) {
        for (annotation in valueParameter.klibAnnotations) {
            proto.addExtension(KlibMetadataProtoBuf.parameterAnnotation, annotation.writeAnnotation(c.strings).build())
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
