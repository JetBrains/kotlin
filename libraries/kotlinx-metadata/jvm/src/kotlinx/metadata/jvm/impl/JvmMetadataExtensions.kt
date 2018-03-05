/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import kotlinx.metadata.impl.readAnnotation
import kotlinx.metadata.jvm.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

internal class JvmMetadataExtensions : MetadataExtensions {
    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, strings, types))
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
            fieldSignature?.name,
            fieldSignature?.desc,
            getterSignature?.run { strings.getString(name) + strings.getString(desc) },
            setterSignature?.run { strings.getString(name) + strings.getString(desc) }
        )

        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        ext.visitSyntheticMethodForAnnotations(syntheticMethod?.run { strings.getString(name) + strings.getString(desc) })

        ext.visitEnd()
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, strings: NameResolver, types: TypeTable) {
        val ext = v.visitExtensions(JvmConstructorExtensionVisitor.TYPE) as? JvmConstructorExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmConstructorSignature(proto, strings, types))
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
}
