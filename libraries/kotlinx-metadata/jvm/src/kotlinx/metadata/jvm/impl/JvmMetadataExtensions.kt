/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.impl.readAnnotation
import kotlinx.metadata.impl.writeAnnotation
import kotlinx.metadata.jvm.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.serialization.StringTable

internal class JvmMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmClassExtensionVisitor.TYPE) as? JvmClassExtensionVisitor ?: return

        val anonymousObjectOriginName = proto.getExtensionOrNull(JvmProtoBuf.anonymousObjectOriginName)
        if (anonymousObjectOriginName != null) {
            ext.visitAnonymousObjectOriginName(strings.getString(anonymousObjectOriginName))
        }
    }

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, strings, types)?.wrapAsPublic())

        val lambdaClassOriginName = proto.getExtensionOrNull(JvmProtoBuf.lambdaClassOriginName)
        if (lambdaClassOriginName != null) {
            ext.visitLambdaClassOriginName(strings.getString(lambdaClassOriginName))
        }
    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmPropertyExtensionVisitor.TYPE) as? JvmPropertyExtensionVisitor ?: return
        val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, strings, types)
        val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
        val getterSignature =
            if (propertySignature != null && propertySignature.hasGetter()) propertySignature.getter else null
        val setterSignature =
            if (propertySignature != null && propertySignature.hasSetter()) propertySignature.setter else null
        ext.visit(
            fieldSignature?.wrapAsPublic(),
            getterSignature?.run { JvmMemberSignature(strings.getString(name), strings.getString(desc)) },
            setterSignature?.run { JvmMemberSignature(strings.getString(name), strings.getString(desc)) }
        )

        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        ext.visitSyntheticMethodForAnnotations(syntheticMethod?.run { JvmMemberSignature(strings.getString(name), strings.getString(desc)) })

        ext.visitEnd()
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmConstructorExtensionVisitor.TYPE) as? JvmConstructorExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmConstructorSignature(proto, strings, types)?.wrapAsPublic())
    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, strings: NameResolver) {
        val ext = v.visitExtensions(JvmTypeParameterExtensionVisitor.TYPE) as? JvmTypeParameterExtensionVisitor ?: return
        for (annotation in proto.getExtension(JvmProtoBuf.typeParameterAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(strings))
        }
        ext.visitEnd()
    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, strings: NameResolver) {
        val ext = v.visitExtensions(JvmTypeExtensionVisitor.TYPE) as? JvmTypeExtensionVisitor ?: return
        ext.visit(proto.getExtension(JvmProtoBuf.isRaw))
        for (annotation in proto.getExtension(JvmProtoBuf.typeAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(strings))
        }
        ext.visitEnd()
    }

    override fun writeClassExtensions(
        type: KmExtensionType, proto: ProtoBuf.Class.Builder, strings: StringTable
    ): KmClassExtensionVisitor? {
        if (type != JvmClassExtensionVisitor.TYPE) return null
        return object : JvmClassExtensionVisitor() {
            override fun visitAnonymousObjectOriginName(internalName: String) {
                proto.setExtension(JvmProtoBuf.anonymousObjectOriginName, strings.getStringIndex(internalName))
            }
        }
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType, proto: ProtoBuf.Function.Builder, strings: StringTable
    ): KmFunctionExtensionVisitor? {
        if (type != JvmFunctionExtensionVisitor.TYPE) return null
        return object : JvmFunctionExtensionVisitor() {
            override fun visit(desc: JvmMemberSignature?) {
                if (desc != null) {
                    proto.setExtension(JvmProtoBuf.methodSignature, desc.toJvmMethodSignature(strings))
                }
            }

            override fun visitLambdaClassOriginName(internalName: String) {
                proto.setExtension(JvmProtoBuf.lambdaClassOriginName, strings.getStringIndex(internalName))
            }
        }
    }

    override fun writePropertyExtensions(
        type: KmExtensionType, proto: ProtoBuf.Property.Builder, strings: StringTable
    ): KmPropertyExtensionVisitor? {
        if (type != JvmPropertyExtensionVisitor.TYPE) return null
        return object : JvmPropertyExtensionVisitor() {
            var signature: JvmProtoBuf.JvmPropertySignature.Builder? = null

            override fun visit(fieldDesc: JvmMemberSignature?, getterDesc: JvmMemberSignature?, setterDesc: JvmMemberSignature?) {
                if (fieldDesc == null && getterDesc == null && setterDesc == null) return

                if (signature == null) {
                    signature = JvmProtoBuf.JvmPropertySignature.newBuilder()
                }
                signature!!.apply {
                    if (fieldDesc != null) {
                        field = JvmProtoBuf.JvmFieldSignature.newBuilder().also { field ->
                            field.name = strings.getStringIndex(fieldDesc.name)
                            field.desc = strings.getStringIndex(fieldDesc.desc)
                        }.build()
                    }
                    if (getterDesc != null) {
                        getter = getterDesc.toJvmMethodSignature(strings)
                    }
                    if (setterDesc != null) {
                        setter = setterDesc.toJvmMethodSignature(strings)
                    }
                }
            }

            override fun visitSyntheticMethodForAnnotations(desc: JvmMemberSignature?) {
                if (desc == null) return

                if (signature == null) {
                    signature = JvmProtoBuf.JvmPropertySignature.newBuilder()
                }

                signature!!.syntheticMethod = desc.toJvmMethodSignature(strings)
            }

            override fun visitEnd() {
                if (signature != null) {
                    proto.setExtension(JvmProtoBuf.propertySignature, signature!!.build())
                }
            }
        }
    }

    override fun writeConstructorExtensions(
        type: KmExtensionType, proto: ProtoBuf.Constructor.Builder, strings: StringTable
    ): KmConstructorExtensionVisitor? {
        if (type != JvmConstructorExtensionVisitor.TYPE) return null
        return object : JvmConstructorExtensionVisitor() {
            override fun visit(desc: JvmMemberSignature?) {
                if (desc != null) {
                    proto.setExtension(JvmProtoBuf.constructorSignature, desc.toJvmMethodSignature(strings))
                }
            }
        }
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType, proto: ProtoBuf.TypeParameter.Builder, strings: StringTable
    ): KmTypeParameterExtensionVisitor? {
        if (type != JvmTypeParameterExtensionVisitor.TYPE) return null
        return object : JvmTypeParameterExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(strings).build())
            }
        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, strings: StringTable): KmTypeExtensionVisitor? {
        if (type != JvmTypeExtensionVisitor.TYPE) return null
        return object : JvmTypeExtensionVisitor() {
            override fun visit(isRaw: Boolean) {
                if (isRaw) {
                    proto.setExtension(JvmProtoBuf.isRaw, true)
                }
            }

            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(JvmProtoBuf.typeAnnotation, annotation.writeAnnotation(strings).build())
            }
        }
    }

    private fun JvmMemberSignature.toJvmMethodSignature(strings: StringTable): JvmProtoBuf.JvmMethodSignature =
        JvmProtoBuf.JvmMethodSignature.newBuilder().apply {
            name = strings.getStringIndex(this@toJvmMethodSignature.name)
            desc = strings.getStringIndex(this@toJvmMethodSignature.desc)
        }.build()
}
