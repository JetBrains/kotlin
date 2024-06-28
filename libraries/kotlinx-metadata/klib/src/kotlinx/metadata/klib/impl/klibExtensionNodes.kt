/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlinx.metadata.klib.impl

import kotlinx.metadata.klib.*
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.*

internal val KmFunction.klibExtensions: KlibFunctionExtension
    get() = getExtension(KlibFunctionExtensionVisitor.TYPE) as KlibFunctionExtension

internal val KmClass.klibExtensions: KlibClassExtension
    get() = getExtension(KlibClassExtensionVisitor.TYPE) as KlibClassExtension

internal val KmType.klibExtensions: KlibTypeExtension
    get() = getExtension(KlibTypeExtensionVisitor.TYPE) as KlibTypeExtension

internal val KmProperty.klibExtensions: KlibPropertyExtension
    get() = getExtension(KlibPropertyExtensionVisitor.TYPE) as KlibPropertyExtension

internal val KmConstructor.klibExtensions: KlibConstructorExtension
    get() = getExtension(KlibConstructorExtensionVisitor.TYPE) as KlibConstructorExtension

internal val KmTypeParameter.klibExtensions: KlibTypeParameterExtension
    get() = getExtension(KlibTypeParameterExtensionVisitor.TYPE) as KlibTypeParameterExtension

internal val KmPackage.klibExtensions: KlibPackageExtension
    get() = getExtension(KlibPackageExtensionVisitor.TYPE) as KlibPackageExtension

internal val KmModuleFragment.klibExtensions: KlibModuleFragmentExtension
    get() = getExtension(KlibModuleFragmentExtensionVisitor.TYPE) as KlibModuleFragmentExtension

internal val KmTypeAlias.klibExtensions: KlibTypeAliasExtension
    get() = getExtension(KlibTypeAliasExtensionVisitor.TYPE) as KlibTypeAliasExtension

internal val KmValueParameter.klibExtensions: KlibValueParameterExtension
    get() = getExtension(KlibValueParameterExtensionVisitor.TYPE) as KlibValueParameterExtension

internal class KlibFunctionExtension : KlibFunctionExtensionVisitor(), KmFunctionExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null
    var file: KlibSourceFile? = null

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitFile(file: KlibSourceFile) {
        this.file = file
    }

    fun accept(visitor: KmFunctionExtension) {
        require(visitor is KlibFunctionExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        uniqId?.let(visitor::visitUniqId)
        file?.let(visitor::visitFile)
    }
}

internal class KlibClassExtension : KlibClassExtensionVisitor(), KmClassExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    val enumEntries: MutableList<KlibEnumEntry> = mutableListOf()
    var uniqId: UniqId? = null
    var file: KlibSourceFile? = null

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    override fun visitFile(file: KlibSourceFile) {
        this.file = file
    }

    override fun visitEnumEntry(entry: KlibEnumEntry) {
        enumEntries += entry
    }

    fun accept(visitor: KmClassExtension) {
        require(visitor is KlibClassExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        enumEntries.forEach(visitor::visitEnumEntry)
        uniqId?.let(visitor::visitUniqId)
        file?.let(visitor::visitFile)
    }
}

internal class KlibTypeExtension : KlibTypeExtensionVisitor(), KmTypeExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    fun accept(visitor: KmTypeExtension) {
        require(visitor is KlibTypeExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }

    override fun hashCode(): Int {
        return annotations.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KlibTypeExtension

        return annotations == other.annotations
    }
}

internal class KlibPropertyExtension : KlibPropertyExtensionVisitor(), KmPropertyExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    val getterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    val setterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null
    var file: Int? = null
    var compileTimeValue: KmAnnotationArgument? = null

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitGetterAnnotation(annotation: KmAnnotation) {
        getterAnnotations += annotation
    }

    override fun visitSetterAnnotation(annotation: KmAnnotation) {
        setterAnnotations += annotation
    }

    override fun visitFile(file: Int) {
        this.file = file
    }

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    override fun visitCompileTimeValue(value: KmAnnotationArgument) {
        this.compileTimeValue = value
    }

    fun accept(visitor: KmPropertyExtension) {
        require(visitor is KlibPropertyExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        getterAnnotations.forEach(visitor::visitGetterAnnotation)
        setterAnnotations.forEach(visitor::visitSetterAnnotation)
        file?.let(visitor::visitFile)
        uniqId?.let(visitor::visitUniqId)
        compileTimeValue?.let(visitor::visitCompileTimeValue)
    }
}

internal class KlibConstructorExtension : KlibConstructorExtensionVisitor(), KmConstructorExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    fun accept(visitor: KmConstructorExtension) {
        require(visitor is KlibConstructorExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        uniqId?.let(visitor::visitUniqId)
    }
}

internal class KlibTypeParameterExtension : KlibTypeParameterExtensionVisitor(), KmTypeParameterExtension {

    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    fun accept(visitor: KmTypeParameterExtension) {
        require(visitor is KlibTypeParameterExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
        uniqId?.let(visitor::visitUniqId)
    }
}

internal class KlibPackageExtension : KlibPackageExtensionVisitor(), KmPackageExtension {

    var fqName: String? = null

    override fun visitFqName(name: String) {
        fqName = name
    }

    fun accept(visitor: KmPackageExtension) {
        require(visitor is KlibPackageExtensionVisitor)
        fqName?.let(visitor::visitFqName)
    }
}

internal class KlibModuleFragmentExtension : KlibModuleFragmentExtensionVisitor(), KmModuleFragmentExtension {

    val moduleFragmentFiles: MutableList<KlibSourceFile> = ArrayList()
    var fqName: String? = null
    val className: MutableList<ClassName> = ArrayList()

    override fun visitFile(file: KlibSourceFile) {
        moduleFragmentFiles += file
    }

    override fun visitFqName(fqName: String) {
        this.fqName = fqName
    }

    override fun visitClassName(className: ClassName) {
        this.className += className
    }

    fun accept(visitor: KmModuleFragmentExtension) {
        require(visitor is KlibModuleFragmentExtensionVisitor)
        moduleFragmentFiles.forEach(visitor::visitFile)
        fqName?.let(visitor::visitFqName)
        className.forEach(visitor::visitClassName)
    }
}

internal class KlibTypeAliasExtension : KlibTypeAliasExtensionVisitor(), KmTypeAliasExtension {
    var uniqId: UniqId? = null

    override fun visitUniqId(uniqId: UniqId) {
        this.uniqId = uniqId
    }

    fun accept(visitor: KmTypeAliasExtension) {
        require(visitor is KlibTypeAliasExtensionVisitor)
        uniqId?.let(visitor::visitUniqId)
    }
}

internal class KlibValueParameterExtension : KlibValueParameterExtensionVisitor(), KmValueParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations += annotation
    }

    fun accept(visitor: KmValueParameterExtension) {
        require(visitor is KlibValueParameterExtensionVisitor)
        annotations.forEach(visitor::visitAnnotation)
    }
}
