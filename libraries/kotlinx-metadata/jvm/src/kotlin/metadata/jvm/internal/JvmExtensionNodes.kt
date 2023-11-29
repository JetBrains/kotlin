/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION","DEPRECATION_ERROR") // inheritance of deprecated visitors will be removed with visitors

package kotlin.metadata.jvm.internal

import kotlin.metadata.*
import kotlin.metadata.internal.extensions.*
import kotlin.metadata.jvm.*

internal val KmClass.jvm: JvmClassExtension
    get() = visitExtensions(JvmClassExtensionVisitor.TYPE) as JvmClassExtension

internal val KmPackage.jvm: JvmPackageExtension
    get() = visitExtensions(JvmPackageExtensionVisitor.TYPE) as JvmPackageExtension

internal val KmFunction.jvm: JvmFunctionExtension
    get() = visitExtensions(JvmFunctionExtensionVisitor.TYPE) as JvmFunctionExtension

internal val KmProperty.jvm: JvmPropertyExtension
    get() = visitExtensions(JvmPropertyExtensionVisitor.TYPE) as JvmPropertyExtension

internal val KmConstructor.jvm: JvmConstructorExtension
    get() = visitExtensions(JvmConstructorExtensionVisitor.TYPE) as JvmConstructorExtension

internal val KmTypeParameter.jvm: JvmTypeParameterExtension
    get() = visitExtensions(JvmTypeParameterExtensionVisitor.TYPE) as JvmTypeParameterExtension

internal val KmType.jvm: JvmTypeExtension
    get() = visitExtensions(JvmTypeExtensionVisitor.TYPE) as JvmTypeExtension


internal class JvmClassExtension : JvmClassExtensionVisitor(), KmClassExtension {
    val localDelegatedProperties: MutableList<KmProperty> = ArrayList(0)
    var moduleName: String? = null
    var anonymousObjectOriginName: String? = null
    var jvmFlags: Int = 0

    override fun visitLocalDelegatedProperty(flags: Int, name: String, getterFlags: Int, setterFlags: Int): KmPropertyVisitor =
        KmProperty(flags, name, getterFlags, setterFlags).also { localDelegatedProperties.add(it) }

    override fun visitModuleName(name: String) {
        this.moduleName = name
    }

    override fun visitAnonymousObjectOriginName(internalName: String) {
        this.anonymousObjectOriginName = internalName
    }

    override fun visitJvmFlags(flags: Int) {
        this.jvmFlags = flags
    }

    override fun accept(visitor: KmClassExtensionVisitor) {
        require(visitor is JvmClassExtensionVisitor)
        localDelegatedProperties.forEach {
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // getter.flags
            visitor.visitLocalDelegatedProperty(it.flags, it.name, it.getter.flags, it.setterFlags)?.let(it::accept)
        }
        moduleName?.let(visitor::visitModuleName)
        anonymousObjectOriginName?.let(visitor::visitAnonymousObjectOriginName)
        jvmFlags.takeIf { it != 0 }?.let(visitor::visitJvmFlags)
        visitor.visitEnd()
    }
}

internal class JvmPackageExtension : JvmPackageExtensionVisitor(), KmPackageExtension {
    val localDelegatedProperties: MutableList<KmProperty> = ArrayList(0)
    var moduleName: String? = null

    override fun visitLocalDelegatedProperty(flags: Int, name: String, getterFlags: Int, setterFlags: Int): KmPropertyVisitor =
        KmProperty(flags, name, getterFlags, setterFlags).also { localDelegatedProperties.add(it) }

    override fun visitModuleName(name: String) {
        this.moduleName = name
    }

    override fun accept(visitor: KmPackageExtensionVisitor) {
        require(visitor is JvmPackageExtensionVisitor)
        localDelegatedProperties.forEach {
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // getter.flags
            visitor.visitLocalDelegatedProperty(it.flags, it.name, it.getter.flags, it.setterFlags)?.let(it::accept)
        }
        moduleName?.let(visitor::visitModuleName)
        visitor.visitEnd()
    }
}

internal class JvmFunctionExtension : JvmFunctionExtensionVisitor(), KmFunctionExtension {
    var signature: JvmMethodSignature? = null
    var lambdaClassOriginName: String? = null

    override fun visit(signature: JvmMethodSignature?) {
        this.signature = signature
    }

    override fun visitLambdaClassOriginName(internalName: String) {
        this.lambdaClassOriginName = internalName
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is JvmFunctionExtensionVisitor)
        visitor.visit(signature)
        lambdaClassOriginName?.let(visitor::visitLambdaClassOriginName)
        visitor.visitEnd()
    }
}

internal class JvmPropertyExtension : JvmPropertyExtensionVisitor(), KmPropertyExtension {
    var jvmFlags: Int = 0
    var fieldSignature: JvmFieldSignature? = null
    var getterSignature: JvmMethodSignature? = null
    var setterSignature: JvmMethodSignature? = null
    var syntheticMethodForAnnotations: JvmMethodSignature? = null
    var syntheticMethodForDelegate: JvmMethodSignature? = null

    override fun visit(
        jvmFlags: Int,
        fieldSignature: JvmFieldSignature?,
        getterSignature: JvmMethodSignature?,
        setterSignature: JvmMethodSignature?
    ) {
        this.jvmFlags = jvmFlags
        this.fieldSignature = fieldSignature
        this.getterSignature = getterSignature
        this.setterSignature = setterSignature
    }

    override fun visitSyntheticMethodForAnnotations(signature: JvmMethodSignature?) {
        this.syntheticMethodForAnnotations = signature
    }

    override fun visitSyntheticMethodForDelegate(signature: JvmMethodSignature?) {
        this.syntheticMethodForDelegate = signature
    }

    override fun accept(visitor: KmPropertyExtensionVisitor) {
        require(visitor is JvmPropertyExtensionVisitor)
        visitor.visit(jvmFlags, fieldSignature, getterSignature, setterSignature)
        visitor.visitSyntheticMethodForAnnotations(syntheticMethodForAnnotations)
        visitor.visitSyntheticMethodForDelegate(syntheticMethodForDelegate)
        visitor.visitEnd()
    }
}

internal class JvmConstructorExtension : JvmConstructorExtensionVisitor(), KmConstructorExtension {
    var signature: JvmMethodSignature? = null

    override fun visit(signature: JvmMethodSignature?) {
        this.signature = signature
    }

    override fun accept(visitor: KmConstructorExtensionVisitor) {
        require(visitor is JvmConstructorExtensionVisitor)
        visitor.visit(signature)
    }
}

internal class JvmTypeParameterExtension : JvmTypeParameterExtensionVisitor(), KmTypeParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    override fun accept(visitor: KmTypeParameterExtensionVisitor) {
        require(visitor is JvmTypeParameterExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        visitor.visitEnd()
    }
}

internal class JvmTypeExtension : JvmTypeExtensionVisitor(), KmTypeExtension {
    var isRaw: Boolean = false
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visit(isRaw: Boolean) {
        this.isRaw = isRaw
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    override fun accept(visitor: KmTypeExtensionVisitor) {
        require(visitor is JvmTypeExtensionVisitor)
        visitor.visit(isRaw)
        annotations.forEach(visitor::visitAnnotation)
        visitor.visitEnd()
    }
}
