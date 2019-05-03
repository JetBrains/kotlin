/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.jvm.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

internal class JvmMetadataExtensions : MetadataExtensions {
    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext) {
        val ext = v.visitExtensions(JvmClassExtensionVisitor.TYPE) as? JvmClassExtensionVisitor ?: return

        val anonymousObjectOriginName = proto.getExtensionOrNull(JvmProtoBuf.anonymousObjectOriginName)
        if (anonymousObjectOriginName != null) {
            ext.visitAnonymousObjectOriginName(c[anonymousObjectOriginName])
        }

        for (property in proto.getExtension(JvmProtoBuf.classLocalVariable)) {
            ext.visitLocalDelegatedProperty(
                property.flags, c[property.name], property.getPropertyGetterFlags(), property.getPropertySetterFlags()
            )?.let { property.accept(it, c) }
        }

        ext.visitModuleName(proto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        ext.visitEnd()
    }

    override fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext) {
        val ext = v.visitExtensions(JvmPackageExtensionVisitor.TYPE) as? JvmPackageExtensionVisitor ?: return

        for (property in proto.getExtension(JvmProtoBuf.packageLocalVariable)) {
            ext.visitLocalDelegatedProperty(
                property.flags, c[property.name], property.getPropertyGetterFlags(), property.getPropertySetterFlags()
            )?.let { property.accept(it, c) }
        }

        ext.visitModuleName(proto.getExtensionOrNull(JvmProtoBuf.packageModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        ext.visitEnd()
    }

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, c.strings, c.types)?.wrapAsPublic())

        val lambdaClassOriginName = proto.getExtensionOrNull(JvmProtoBuf.lambdaClassOriginName)
        if (lambdaClassOriginName != null) {
            ext.visitLambdaClassOriginName(c[lambdaClassOriginName])
        }

        ext.visitEnd()
    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext) {
        val ext = v.visitExtensions(JvmPropertyExtensionVisitor.TYPE) as? JvmPropertyExtensionVisitor ?: return
        val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, c.strings, c.types)
        val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
        val getterSignature =
            if (propertySignature != null && propertySignature.hasGetter()) propertySignature.getter else null
        val setterSignature =
            if (propertySignature != null && propertySignature.hasSetter()) propertySignature.setter else null
        ext.visit(
            fieldSignature?.wrapAsPublic(),
            getterSignature?.run { JvmMethodSignature(c[name], c[desc]) },
            setterSignature?.run { JvmMethodSignature(c[name], c[desc]) }
        )

        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        ext.visitSyntheticMethodForAnnotations(syntheticMethod?.run { JvmMethodSignature(c[name], c[desc]) })

        ext.visitEnd()
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext) {
        val ext = v.visitExtensions(JvmConstructorExtensionVisitor.TYPE) as? JvmConstructorExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmConstructorSignature(proto, c.strings, c.types)?.wrapAsPublic())
    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val ext = v.visitExtensions(JvmTypeParameterExtensionVisitor.TYPE) as? JvmTypeParameterExtensionVisitor ?: return
        for (annotation in proto.getExtension(JvmProtoBuf.typeParameterAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        ext.visitEnd()
    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext) {
        val ext = v.visitExtensions(JvmTypeExtensionVisitor.TYPE) as? JvmTypeExtensionVisitor ?: return
        ext.visit(proto.getExtension(JvmProtoBuf.isRaw))
        for (annotation in proto.getExtension(JvmProtoBuf.typeAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        ext.visitEnd()
    }

    override fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor? {
        if (type != JvmClassExtensionVisitor.TYPE) return null
        return object : JvmClassExtensionVisitor() {
            override fun visitAnonymousObjectOriginName(internalName: String) {
                proto.setExtension(JvmProtoBuf.anonymousObjectOriginName, c[internalName])
            }

            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor = writeProperty(c, flags, name, getterFlags, setterFlags) {
                proto.addExtension(JvmProtoBuf.classLocalVariable, it.build())
            }

            override fun visitModuleName(name: String) {
                if (name != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
                    proto.setExtension(JvmProtoBuf.classModuleName, c[name])
                }
            }
        }
    }

    override fun writePackageExtensions(
        type: KmExtensionType, proto: ProtoBuf.Package.Builder, c: WriteContext
    ): KmPackageExtensionVisitor? {
        if (type != JvmPackageExtensionVisitor.TYPE) return null
        return object : JvmPackageExtensionVisitor() {
            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor = writeProperty(c, flags, name, getterFlags, setterFlags) {
                proto.addExtension(JvmProtoBuf.packageLocalVariable, it.build())
            }

            override fun visitModuleName(name: String) {
                if (name != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
                    proto.setExtension(JvmProtoBuf.packageModuleName, c[name])
                }
            }
        }
    }

    override fun writeFunctionExtensions(
        type: KmExtensionType, proto: ProtoBuf.Function.Builder, c: WriteContext
    ): KmFunctionExtensionVisitor? {
        if (type != JvmFunctionExtensionVisitor.TYPE) return null
        return object : JvmFunctionExtensionVisitor() {
            override fun visit(desc: JvmMethodSignature?) {
                if (desc != null) {
                    proto.setExtension(JvmProtoBuf.methodSignature, desc.toJvmMethodSignature(c))
                }
            }

            override fun visitLambdaClassOriginName(internalName: String) {
                proto.setExtension(JvmProtoBuf.lambdaClassOriginName, c[internalName])
            }
        }
    }

    override fun writePropertyExtensions(
        type: KmExtensionType, proto: ProtoBuf.Property.Builder, c: WriteContext
    ): KmPropertyExtensionVisitor? {
        if (type != JvmPropertyExtensionVisitor.TYPE) return null
        return object : JvmPropertyExtensionVisitor() {
            var signature: JvmProtoBuf.JvmPropertySignature.Builder? = null

            override fun visit(fieldDesc: JvmFieldSignature?, getterDesc: JvmMethodSignature?, setterDesc: JvmMethodSignature?) {
                if (fieldDesc == null && getterDesc == null && setterDesc == null) return

                if (signature == null) {
                    signature = JvmProtoBuf.JvmPropertySignature.newBuilder()
                }
                signature!!.apply {
                    if (fieldDesc != null) {
                        field = JvmProtoBuf.JvmFieldSignature.newBuilder().also { field ->
                            field.name = c[fieldDesc.name]
                            field.desc = c[fieldDesc.desc]
                        }.build()
                    }
                    if (getterDesc != null) {
                        getter = getterDesc.toJvmMethodSignature(c)
                    }
                    if (setterDesc != null) {
                        setter = setterDesc.toJvmMethodSignature(c)
                    }
                }
            }

            override fun visitSyntheticMethodForAnnotations(desc: JvmMethodSignature?) {
                if (desc == null) return

                if (signature == null) {
                    signature = JvmProtoBuf.JvmPropertySignature.newBuilder()
                }

                signature!!.syntheticMethod = desc.toJvmMethodSignature(c)
            }

            override fun visitEnd() {
                if (signature != null) {
                    proto.setExtension(JvmProtoBuf.propertySignature, signature!!.build())
                }
            }
        }
    }

    override fun writeConstructorExtensions(
        type: KmExtensionType, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    ): KmConstructorExtensionVisitor? {
        if (type != JvmConstructorExtensionVisitor.TYPE) return null
        return object : JvmConstructorExtensionVisitor() {
            override fun visit(desc: JvmMethodSignature?) {
                if (desc != null) {
                    proto.setExtension(JvmProtoBuf.constructorSignature, desc.toJvmMethodSignature(c))
                }
            }
        }
    }

    override fun writeTypeParameterExtensions(
        type: KmExtensionType, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    ): KmTypeParameterExtensionVisitor? {
        if (type != JvmTypeParameterExtensionVisitor.TYPE) return null
        return object : JvmTypeParameterExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotation.writeAnnotation(c.strings).build())
            }
        }
    }

    override fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor? {
        if (type != JvmTypeExtensionVisitor.TYPE) return null
        return object : JvmTypeExtensionVisitor() {
            override fun visit(isRaw: Boolean) {
                if (isRaw) {
                    proto.setExtension(JvmProtoBuf.isRaw, true)
                }
            }

            override fun visitAnnotation(annotation: KmAnnotation) {
                proto.addExtension(JvmProtoBuf.typeAnnotation, annotation.writeAnnotation(c.strings).build())
            }
        }
    }

    private fun JvmMemberSignature.toJvmMethodSignature(c: WriteContext): JvmProtoBuf.JvmMethodSignature =
        JvmProtoBuf.JvmMethodSignature.newBuilder().apply {
            name = c[this@toJvmMethodSignature.name]
            desc = c[this@toJvmMethodSignature.desc]
        }.build()
}
