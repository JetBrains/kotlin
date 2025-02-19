/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.internal.common

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import kotlin.metadata.*
import kotlin.metadata.internal.ReadContext
import kotlin.metadata.internal.WriteContext
import kotlin.metadata.internal.extensions.*
import kotlin.metadata.internal.readAnnotation
import kotlin.metadata.internal.writeAnnotation

public class BuiltInsMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class, c: ReadContext) {
        proto.getExtension(BuiltInsProtoBuf.classAnnotation).forEach { annotation ->
            kmClass.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readPackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package, c: ReadContext) {
        val ext = kmPackage.builtins
        proto.getExtensionOrNull(BuiltInsProtoBuf.packageFqName)?.let {
            ext.fqName = (c.strings as NameResolverImpl).getPackageFqName(it)
        }
    }

    override fun readModuleFragmentExtensions(kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment, c: ReadContext) {
    }

    override fun readFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function, c: ReadContext) {
        proto.getExtension(BuiltInsProtoBuf.functionAnnotation).forEach { annotation ->
            kmFunction.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readPropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property, c: ReadContext) {
        proto.getExtension(BuiltInsProtoBuf.propertyAnnotation).forEach { annotation ->
            kmProperty.annotations.add(annotation.readAnnotation(c.strings))
        }
        proto.getExtension(BuiltInsProtoBuf.propertyGetterAnnotation).forEach { annotation ->
            kmProperty.getter.annotations.add(annotation.readAnnotation(c.strings))
        }
        kmProperty.setter?.let { setter ->
            proto.getExtension(BuiltInsProtoBuf.propertySetterAnnotation).forEach { annotation ->
                setter.annotations.add(annotation.readAnnotation(c.strings))
            }
        }
    }

    override fun readConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor, c: ReadContext) {
        proto.getExtension(BuiltInsProtoBuf.constructorAnnotation).forEach { annotation ->
            kmConstructor.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val ext = kmTypeParameter.builtins
        proto.getExtension(BuiltInsProtoBuf.typeParameterAnnotation).forEach { annotation ->
            ext.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeExtensions(kmType: KmType, proto: ProtoBuf.Type, c: ReadContext) {
        val ext = kmType.builtins
        proto.getExtension(BuiltInsProtoBuf.typeAnnotation).forEach { annotation ->
            ext.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeAliasExtensions(kmTypeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext) {
    }

    override fun readValueParameterExtensions(kmValueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter, c: ReadContext) {
        proto.getExtension(BuiltInsProtoBuf.parameterAnnotation).forEach { annotation ->
            kmValueParameter.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun writeClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class.Builder, c: WriteContext) {
        for (annotation in kmClass.annotations) {
            proto.addExtension(BuiltInsProtoBuf.classAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writePackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext) {
        extension { kmPackage.fqName }?.let { fqName ->
            proto.setExtension(BuiltInsProtoBuf.packageFqName, c.strings.getPackageFqNameIndexByString(fqName))
        }
    }

    override fun writeModuleFragmentExtensions(
        kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext,
    ) {
    }

    override fun writeFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext) {
        for (annotation in kmFunction.annotations) {
            proto.addExtension(BuiltInsProtoBuf.functionAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writePropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext) {
        for (annotation in kmProperty.annotations) {
            proto.addExtension(BuiltInsProtoBuf.propertyAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.getter.annotations) {
            proto.addExtension(BuiltInsProtoBuf.propertyGetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
        for (annotation in kmProperty.setter?.annotations.orEmpty()) {
            proto.addExtension(BuiltInsProtoBuf.propertySetterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext) {
        for (annotation in kmConstructor.annotations) {
            proto.addExtension(BuiltInsProtoBuf.constructorAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext) {
        for (annotation in extension { kmTypeParameter.annotations }) {
            proto.addExtension(BuiltInsProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeTypeExtensions(type: KmType, proto: ProtoBuf.Type.Builder, c: WriteContext) {
        for (annotation in extension { type.annotations }) {
            proto.addExtension(BuiltInsProtoBuf.typeAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeTypeAliasExtensions(typeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext) {
    }

    override fun writeValueParameterExtensions(valueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext) {
        for (annotation in valueParameter.annotations) {
            proto.addExtension(BuiltInsProtoBuf.parameterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun createClassExtension(): KmClassExtension = BuiltInClassExtension()

    override fun createPackageExtension(): KmPackageExtension = BuiltInPackageExtension()

    override fun createModuleFragmentExtensions(): KmModuleFragmentExtension =
        object : KmModuleFragmentExtension {
            override val type: KmExtensionType = KmExtensionType(KmModuleFragmentExtension::class)
        }

    override fun createFunctionExtension(): KmFunctionExtension = BuiltInFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension = BuiltInPropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension = BuiltInConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension = BuiltInTypeParameterExtension()

    override fun createTypeExtension(): KmTypeExtension = BuiltInTypeExtension()

    override fun createTypeAliasExtension(): KmTypeAliasExtension? = null

    override fun createValueParameterExtension(): KmValueParameterExtension? = BuiltInValueParameterExtension()

    private inline fun <T> extension(lambda: BuiltInExtensionsAccessor.() -> T): T = with(BuiltInExtensionsAccessor, lambda)
}
