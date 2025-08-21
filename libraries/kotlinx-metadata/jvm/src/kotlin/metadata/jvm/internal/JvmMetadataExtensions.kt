/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.jvm.internal

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import kotlin.metadata.*
import kotlin.metadata.internal.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.*
import kotlin.metadata.jvm.JvmMemberSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.wrapAsPublic

@OptIn(ExperimentalAnnotationsInMetadata::class)
internal class JvmMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class, c: ReadContext) {
        val ext = kmClass.jvm

        proto.annotationList.mapTo(kmClass.annotations) { it.readAnnotation(c.strings) }

        val anonymousObjectOriginName = proto.getExtensionOrNull(JvmProtoBuf.anonymousObjectOriginName)
        if (anonymousObjectOriginName != null) {
            ext.anonymousObjectOriginName = c[anonymousObjectOriginName]
        }

        for (property in proto.getExtension(JvmProtoBuf.classLocalVariable)) {
            ext.localDelegatedProperties.add(property.toKmProperty(c))
        }

        ext.moduleName = proto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME

        proto.getExtensionOrNull(JvmProtoBuf.jvmClassFlags)?.let {
            ext.jvmFlags = it
        }
    }

    override fun readPackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package, c: ReadContext) {
        val ext = kmPackage.jvm

        for (property in proto.getExtension(JvmProtoBuf.packageLocalVariable)) {
            ext.localDelegatedProperties.add(property.toKmProperty(c))
        }

        ext.moduleName = proto.getExtensionOrNull(JvmProtoBuf.packageModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
    }

    // ModuleFragment is not used by JVM backend.
    override fun readModuleFragmentExtensions(kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment, c: ReadContext) {}

    override fun readFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function, c: ReadContext) {
        val ext = kmFunction.jvm
        proto.annotationList.mapTo(kmFunction.annotations) { it.readAnnotation(c.strings) }
        proto.extensionReceiverAnnotationList.mapTo(kmFunction.extensionReceiverParameterAnnotations) { it.readAnnotation(c.strings) }
        ext.signature = JvmProtoBufUtil.getJvmMethodSignature(proto, c.strings, c.types)?.wrapAsPublic()

        val lambdaClassOriginName = proto.getExtensionOrNull(JvmProtoBuf.lambdaClassOriginName)
        if (lambdaClassOriginName != null) {
            ext.lambdaClassOriginName = c[lambdaClassOriginName]
        }
    }

    override fun readPropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property, c: ReadContext) {
        val ext = kmProperty.jvm

        proto.annotationList.mapTo(kmProperty.annotations) { it.readAnnotation(c.strings) }
        proto.getterAnnotationList.mapTo(kmProperty.getter.annotations) { it.readAnnotation(c.strings) }
        kmProperty.setter?.let { setter ->
            proto.setterAnnotationList.mapTo(setter.annotations) { it.readAnnotation(c.strings) }
        }
        proto.extensionReceiverAnnotationList.mapTo(kmProperty.extensionReceiverParameterAnnotations) { it.readAnnotation(c.strings) }
        proto.backingFieldAnnotationList.mapTo(kmProperty.backingFieldAnnotations) { it.readAnnotation(c.strings) }
        proto.delegateFieldAnnotationList.mapTo(kmProperty.delegateFieldAnnotations) { it.readAnnotation(c.strings) }

        val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, c.strings, c.types)
        val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
        val getterSignature =
            if (propertySignature != null && propertySignature.hasGetter()) propertySignature.getter else null
        val setterSignature =
            if (propertySignature != null && propertySignature.hasSetter()) propertySignature.setter else null
        ext.jvmFlags = proto.getExtension(JvmProtoBuf.flags)
        ext.fieldSignature = fieldSignature?.wrapAsPublic()
        ext.getterSignature = getterSignature?.run { JvmMethodSignature(c[name], c[desc]) }
        ext.setterSignature = setterSignature?.run { JvmMethodSignature(c[name], c[desc]) }

        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        ext.syntheticMethodForAnnotations = syntheticMethod?.run { JvmMethodSignature(c[name], c[desc]) }

        val delegateMethod =
            if (propertySignature != null && propertySignature.hasDelegateMethod()) propertySignature.delegateMethod else null
        ext.syntheticMethodForDelegate = delegateMethod?.run { JvmMethodSignature(c[name], c[desc]) }
    }

    override fun readConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor, c: ReadContext) {
        val ext = kmConstructor.jvm
        proto.annotationList.mapTo(kmConstructor.annotations) { it.readAnnotation(c.strings) }
        ext.signature = JvmProtoBufUtil.getJvmConstructorSignature(proto, c.strings, c.types)?.wrapAsPublic()
    }

    override fun readTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val ext = kmTypeParameter.jvm
        for (annotation in proto.getExtension(JvmProtoBuf.typeParameterAnnotation)) {
            ext.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readEnumEntryExtensions(kmEnumEntry: KmEnumEntry, proto: ProtoBuf.EnumEntry, c: ReadContext) {
        for (annotation in proto.annotationList) {
            kmEnumEntry.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeExtensions(kmType: KmType, proto: ProtoBuf.Type, c: ReadContext) {
        val ext = kmType.jvm
        ext.isRaw = proto.getExtension(JvmProtoBuf.isRaw)
        for (annotation in proto.getExtension(JvmProtoBuf.typeAnnotation)) {
            ext.annotations.add(annotation.readAnnotation(c.strings))
        }
    }

    override fun readTypeAliasExtensions(kmTypeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext) {}

    override fun readValueParameterExtensions(kmValueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter, c: ReadContext) {
        proto.annotationList.mapTo(kmValueParameter.annotations) { it.readAnnotation(c.strings) }
    }

    override fun writeClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class.Builder, c: WriteContext) =
        with(kmClass.jvm) {
            proto.addAllAnnotation(kmClass.annotations.map { it.writeAnnotation(c.strings).build() })
            anonymousObjectOriginName?.let {
                proto.setExtension(JvmProtoBuf.anonymousObjectOriginName, c[it])
            }
            localDelegatedProperties.forEach {
                proto.addExtension(JvmProtoBuf.classLocalVariable, c.writeProperty(it).build())
            }
            moduleName?.let { moduleName ->
                if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) proto.setExtension(JvmProtoBuf.classModuleName, c[moduleName])
            }
            if (jvmFlags != 0) proto.setExtension(JvmProtoBuf.jvmClassFlags, jvmFlags)
        }

    override fun writePackageExtensions(
        kmPackage: KmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext,
    ): Unit = with(kmPackage.jvm) {
        localDelegatedProperties.forEach {
            proto.addExtension(JvmProtoBuf.packageLocalVariable, c.writeProperty(it).build())
        }
        moduleName?.let { name ->
            if (name != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
                proto.setExtension(JvmProtoBuf.packageModuleName, c[name])
            }
        }
    }

    // PackageFragment is not used by JVM backend.
    override fun writeModuleFragmentExtensions(
        kmModuleFragment: KmModuleFragment,
        proto: ProtoBuf.PackageFragment.Builder,
        c: WriteContext,
    ) = Unit

    override fun writeFunctionExtensions(
        kmFunction: KmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext,
    ) {
        with(kmFunction.jvm) {
            proto.addAllAnnotation(kmFunction.annotations.map { it.writeAnnotation(c.strings).build() })
            proto.addAllExtensionReceiverAnnotation(
                kmFunction.extensionReceiverParameterAnnotations.map { it.writeAnnotation(c.strings).build() }
            )
            signature?.let { proto.setExtension(JvmProtoBuf.methodSignature, it.toJvmMethodSignature(c)) }
            lambdaClassOriginName?.let { proto.setExtension(JvmProtoBuf.lambdaClassOriginName, c[it]) }
        }
    }

    override fun writePropertyExtensions(
        kmProperty: KmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext,
    ) = with(kmProperty.jvm) {
        proto.addAllAnnotation(kmProperty.annotations.map { it.writeAnnotation(c.strings).build() })
        proto.addAllGetterAnnotation(kmProperty.getter.annotations.map { it.writeAnnotation(c.strings).build() })
        kmProperty.setter?.let { setter ->
            proto.addAllSetterAnnotation(setter.annotations.map { it.writeAnnotation(c.strings).build() })
        }
        proto.addAllExtensionReceiverAnnotation(
            kmProperty.extensionReceiverParameterAnnotations.map { it.writeAnnotation(c.strings).build() }
        )
        proto.addAllBackingFieldAnnotation(kmProperty.backingFieldAnnotations.map { it.writeAnnotation(c.strings).build() })
        proto.addAllDelegateFieldAnnotation(kmProperty.delegateFieldAnnotations.map { it.writeAnnotation(c.strings).build() })

        val composedSignature: JvmProtoBuf.JvmPropertySignature.Builder = JvmProtoBuf.JvmPropertySignature.newBuilder()
        var hasSignature = false

        if (fieldSignature != null) {
            hasSignature = true
            composedSignature.field = JvmProtoBuf.JvmFieldSignature.newBuilder().also { field ->
                field.name = c[fieldSignature!!.name]
                field.desc = c[fieldSignature!!.descriptor]
            }.build()
        }
        if (getterSignature != null) {
            hasSignature = true
            composedSignature.getter = getterSignature!!.toJvmMethodSignature(c)
        }
        if (setterSignature != null) {
            hasSignature = true
            composedSignature.setter = setterSignature!!.toJvmMethodSignature(c)
        }
        if (hasSignature && syntheticMethodForAnnotations != null) {
            composedSignature.syntheticMethod = syntheticMethodForAnnotations!!.toJvmMethodSignature(c)
        }
        if (hasSignature && syntheticMethodForDelegate != null) {
            composedSignature.delegateMethod = syntheticMethodForDelegate!!.toJvmMethodSignature(c)
        }
        if (jvmFlags != ProtoBuf.Property.getDefaultInstance().getExtension(JvmProtoBuf.flags)) {
            proto.setExtension(JvmProtoBuf.flags, jvmFlags)
        }
        if (hasSignature) {
            proto.setExtension(JvmProtoBuf.propertySignature, composedSignature.build())
        }
    }

    override fun writeConstructorExtensions(
        kmConstructor: KmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext,
    ): Unit = with(kmConstructor.jvm) {
        proto.addAllAnnotation(kmConstructor.annotations.map { it.writeAnnotation(c.strings).build() })
        signature?.let { proto.setExtension(JvmProtoBuf.constructorSignature, it.toJvmMethodSignature(c)) }
    }

    override fun writeTypeParameterExtensions(
        kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext,
    ) = with(kmTypeParameter.jvm) {
        annotations.forEach { annotation ->
            proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeEnumEntryExtensions(enumEntry: KmEnumEntry, proto: ProtoBuf.EnumEntry.Builder, c: WriteContext) {
        enumEntry.annotations.forEach { annotation ->
            proto.addAnnotation(annotation.writeAnnotation(c.strings).build())
        }
    }

    override fun writeTypeExtensions(type: KmType, proto: ProtoBuf.Type.Builder, c: WriteContext) =
        with(type.jvm) {
            if (isRaw) proto.setExtension(JvmProtoBuf.isRaw, true)
            annotations.forEach { annotation ->
                proto.addExtension(JvmProtoBuf.typeAnnotation, annotation.writeAnnotation(c.strings).build())
            }
        }

    override fun writeTypeAliasExtensions(
        typeAlias: KmTypeAlias,
        proto: ProtoBuf.TypeAlias.Builder,
        c: WriteContext,
    ) = Unit

    override fun writeValueParameterExtensions(
        valueParameter: KmValueParameter,
        proto: ProtoBuf.ValueParameter.Builder,
        c: WriteContext,
    ) {
        proto.addAllAnnotation(valueParameter.annotations.map { it.writeAnnotation(c.strings).build() })
    }

    override fun createClassExtension(): KmClassExtension = JvmClassExtension()

    override fun createPackageExtension(): KmPackageExtension = JvmPackageExtension()

    override fun createModuleFragmentExtensions(): KmModuleFragmentExtension =
        object : KmModuleFragmentExtension {
            override val type: KmExtensionType = KmExtensionType(KmModuleFragmentExtension::class)
        }

    override fun createFunctionExtension(): KmFunctionExtension = JvmFunctionExtension()

    override fun createPropertyExtension(): KmPropertyExtension = JvmPropertyExtension()

    override fun createConstructorExtension(): KmConstructorExtension = JvmConstructorExtension()

    override fun createTypeParameterExtension(): KmTypeParameterExtension = JvmTypeParameterExtension()

    override fun createEnumEntryExtension(): KmEnumEntryExtension? = null

    override fun createTypeExtension(): KmTypeExtension = JvmTypeExtension()

    override fun createTypeAliasExtension(): KmTypeAliasExtension? = null

    override fun createValueParameterExtension(): KmValueParameterExtension? = null

    private fun JvmMemberSignature.toJvmMethodSignature(c: WriteContext): JvmProtoBuf.JvmMethodSignature =
        JvmProtoBuf.JvmMethodSignature.newBuilder().apply {
            name = c[this@toJvmMethodSignature.name]
            desc = c[this@toJvmMethodSignature.descriptor]
        }.build()
}
