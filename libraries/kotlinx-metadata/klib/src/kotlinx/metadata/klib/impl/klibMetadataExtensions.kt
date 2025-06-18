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

@OptIn(ExperimentalAnnotationsInMetadata::class)
internal class KlibMetadataExtensions : MetadataExtensions {

    private fun ReadContext.getSourceFile(index: Int) =
        strings.getString(index).let(::KlibSourceFile)

    private fun WriteContext.getIndexOf(file: KlibSourceFile) =
        strings.getStringIndex(file.name)

    override fun readClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class, c: ReadContext) {
        val extension = kmClass.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.classAnnotation)) {
            kmClass.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.classFile)?.let {
            extension.file = c.getSourceFile(it)
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
            kmFunction.annotations.add(annotation.readAnnotation(c.strings))
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
            kmProperty.annotations.add(annotation.readAnnotation(c.strings))
        }
        for (annotation in proto.getExtension(KlibMetadataProtoBuf.propertyGetterAnnotation)) {
            kmProperty.getter.annotations.add(annotation.readAnnotation(c.strings))
        }
        kmProperty.setter?.let { setter ->
            for (annotation in proto.getExtension(KlibMetadataProtoBuf.propertySetterAnnotation)) {
                setter.annotations.add(annotation.readAnnotation(c.strings))
            }
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile)?.let { file ->
            extension.file = c.getSourceFile(file)
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
            kmConstructor.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.constructorUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
    }

    override fun readTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val extension = kmTypeParameter.klibExtensions

        for (annotation in proto.getExtension(KlibMetadataProtoBuf.typeParameterAnnotation)) {
            kmTypeParameter.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtensionOrNull(KlibMetadataProtoBuf.typeParamUniqId)?.let { uniqId ->
            extension.uniqId = uniqId.readUniqId()
        }
    }

    override fun readEnumEntryExtensions(kmEnumEntry: KmEnumEntry, proto: ProtoBuf.EnumEntry, c: ReadContext) {
        val extension = kmEnumEntry.klibExtensions

        extension.ordinal = proto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryOrdinal)
        extension.uniqId = proto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryUniqId)?.readUniqId()
        for (annotation in proto.getExtension(KlibMetadataProtoBuf.enumEntryAnnotation)) {
            kmEnumEntry.annotations.add(annotation.readAnnotation(c.strings))
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
        for (annotation in proto.getExtension(KlibMetadataProtoBuf.parameterAnnotation)) {
            kmValueParameter.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun writeClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class.Builder, c: WriteContext) {
        for (annotation in kmClass.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.classAnnotation, annotation.writeAnnotation(c.strings).build())
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
        for (annotation in kmFunction.annotations) {
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
        for (annotation in kmProperty.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.propertyAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.getter.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.propertyGetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.setter?.annotations.orEmpty()) {
            proto.addExtension(KlibMetadataProtoBuf.propertySetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }

        kmProperty.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.propertyUniqId, uniqId.writeUniqId().build())
        }

        kmProperty.file?.let { file ->
            proto.setExtension(KlibMetadataProtoBuf.propertyFile, c.getIndexOf(file))
        }

        kmProperty.compileTimeValue?.let { value ->
            proto.setExtension(KlibMetadataProtoBuf.compileTimeValue, value.writeAnnotationArgument(c.strings).build())
        }
    }

    override fun writeConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext) {
        for (annotation in kmConstructor.annotations) {
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

    override fun writeEnumEntryExtensions(enumEntry: KmEnumEntry, proto: ProtoBuf.EnumEntry.Builder, c: WriteContext) {
        enumEntry.ordinal?.let { ordinal ->
            proto.setExtension(KlibMetadataProtoBuf.enumEntryOrdinal, ordinal)
        }
        enumEntry.uniqId?.let { uniqId ->
            proto.setExtension(KlibMetadataProtoBuf.enumEntryUniqId, uniqId.writeUniqId().build())
        }
        for (annotation in enumEntry.annotations) {
            proto.addExtension(KlibMetadataProtoBuf.enumEntryAnnotation, annotation.writeAnnotation(c.strings).build())
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
        for (annotation in valueParameter.annotations) {
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

    override fun createEnumEntryExtension(): KlibEnumEntryExtension? =
        KlibEnumEntryExtension()

    override fun createTypeExtension(): KmTypeExtension =
        KlibTypeExtension()

    override fun createTypeAliasExtension(): KmTypeAliasExtension =
        KlibTypeAliasExtension()

    override fun createValueParameterExtension(): KmValueParameterExtension =
        KlibValueParameterExtension()
}
