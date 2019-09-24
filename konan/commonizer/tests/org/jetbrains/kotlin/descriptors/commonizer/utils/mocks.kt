/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedClassDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.CommonizedTypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.*
import kotlin.random.Random

// expected special name for module
internal fun mockEmptyModule(moduleName: String): ModuleDescriptor {
    val module = KotlinTestUtils.createEmptyModule(moduleName)
    module.initialize(PackageFragmentProvider.Empty)
    return module
}

internal fun mockClassType(
    fqName: String,
    nullable: Boolean = false
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val classFqName = FqName(fqName)

    val classDescriptor = CommonizedClassDescriptor(
        storageManager = LockBasedStorageManager.NO_LOCKS,
        containingDeclaration = createPackageFragmentForClassifier(classFqName),
        annotations = Annotations.EMPTY,
        name = classFqName.shortName(),
        kind = ClassKind.CLASS,
        modality = Modality.FINAL,
        visibility = Visibilities.PUBLIC,
        isCompanion = false,
        isData = false,
        isInline = false,
        isInner = false,
        isExternal = false,
        isExpect = false,
        isActual = false,
        companionObjectName = null,
        supertypes = emptyList()
    )

    classDescriptor.declaredTypeParameters = emptyList()

    classDescriptor.initialize(
        unsubstitutedMemberScope = MemberScope.Empty,
        constructors = emptyList()
    )

    classDescriptor.defaultType.makeNullableAsSpecified(nullable)
}

internal fun mockTAType(
    fqName: String,
    nullable: Boolean = false,
    rightHandSideTypeProvider: () -> KotlinType
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val typeAliasFqName = FqName(fqName)

    val rightHandSideType = rightHandSideTypeProvider().lowerIfFlexible()

    val typeAliasDescriptor = CommonizedTypeAliasDescriptor(
        storageManager = LockBasedStorageManager.NO_LOCKS,
        containingDeclaration = createPackageFragmentForClassifier(typeAliasFqName),
        annotations = Annotations.EMPTY,
        name = typeAliasFqName.shortName(),
        visibility = Visibilities.PUBLIC,
        isActual = false
    )

    typeAliasDescriptor.initialize(
        declaredTypeParameters = emptyList(),
        underlyingType = rightHandSideType.getAbbreviation() ?: rightHandSideType,
        expandedType = rightHandSideType
    )

    (rightHandSideType.getAbbreviatedType()?.expandedType ?: rightHandSideType)
        .withAbbreviation(typeAliasDescriptor.defaultType)
        .makeNullableAsSpecified(nullable)
}

private fun createPackageFragmentForClassifier(classifierFqName: FqName): PackageFragmentDescriptor =
    object : PackageFragmentDescriptor {
        private val module: ModuleDescriptor by lazy { mockEmptyModule("<module4_${classifierFqName.shortName()}_x${Random.nextInt()}>") }
        override fun getContainingDeclaration(): ModuleDescriptor = module
        override val fqName = classifierFqName.parentOrNull() ?: FqName.ROOT
        override fun getMemberScope() = MemberScope.Empty
        override fun getOriginal() = this
        override fun getName() = fqName.shortNameOrSpecial()
        override fun getSource() = SourceElement.NO_SOURCE
        override val annotations = Annotations.EMPTY
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = error("not supported")
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = error("not supported")
        override fun toString() = "package $name"
    }

internal val EMPTY_CLASSIFIERS_CACHE = object : ClassifiersCache {
    override val classes: Map<FqName, ClassNode> get() = emptyMap()
    override val typeAliases: Map<FqName, TypeAliasNode> get() = emptyMap()
}
