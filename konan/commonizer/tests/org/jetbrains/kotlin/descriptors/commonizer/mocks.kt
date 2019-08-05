/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import kotlin.random.Random

// expected special name for module
internal fun mockEmptyModule(moduleName: String): ModuleDescriptor {
    val module = KotlinTestUtils.createEmptyModule(moduleName)
    module.initialize(PackageFragmentProvider.Empty)
    return module
}

@TypeRefinement
internal fun mockClassType(
    fqName: String,
    nullable: Boolean = false
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val classFqName = FqName(fqName)

    val classTypeConstructor = object : AbstractClassTypeConstructor(LockBasedStorageManager.NO_LOCKS) {
        lateinit var classDescriptor: ClassDescriptor
        override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
        override fun computeSupertypes(): List<KotlinType> = emptyList()
        override fun isDenotable() = true
        override fun getDeclarationDescriptor() = classDescriptor
        override val supertypeLoopChecker = SupertypeLoopChecker.EMPTY
        override fun toString() = "class type constructor ${declarationDescriptor.name}"
    }

    val classDescriptor = object : ClassDescriptorBase(
        /*storageManager =*/ LockBasedStorageManager.NO_LOCKS,
        /*containingDeclaration =*/ createPackageFragmentForClassifier(classFqName),
        /*name =*/ classFqName.shortName(),
        /*source =*/ SourceElement.NO_SOURCE,
        /*isExternal =*/ false
    ) {
        override fun getStaticScope() = MemberScope.Empty
        override fun getConstructors(): List<ClassConstructorDescriptor> = emptyList()
        override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
        override fun getKind() = ClassKind.CLASS
        override fun getModality() = Modality.FINAL
        override fun getVisibility() = Visibilities.PUBLIC
        override fun isCompanionObject() = false
        override fun isData() = false
        override fun isInline() = false
        override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null
        override fun isExpect() = false
        override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner) = MemberScope.Empty
        override fun isActual() = false
        override fun getSealedSubclasses(): List<ClassDescriptor> = emptyList()
        override fun getTypeConstructor() = classTypeConstructor
        override fun isInner() = false
        override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = emptyList()
        override val annotations = Annotations.EMPTY
        override fun toString() = "class descriptor $name"
    }

    classTypeConstructor.classDescriptor = classDescriptor

    createSimpleType(classTypeConstructor, nullable)
}

@TypeRefinement
internal fun mockTAType(
    fqName: String,
    nullable: Boolean = false,
    rightHandSideTypeProvider: () -> KotlinType
): KotlinType = LazyWrappedType(LockBasedStorageManager.NO_LOCKS) {
    val typeAliasFqName = FqName(fqName)

    val rightHandSideType = rightHandSideTypeProvider().lowerIfFlexible()

    val typeAliasDescriptor = object : AbstractTypeAliasDescriptor(
        containingDeclaration = createPackageFragmentForClassifier(typeAliasFqName),
        annotations = Annotations.EMPTY,
        name = typeAliasFqName.shortName(),
        sourceElement = SourceElement.NO_SOURCE,
        visibilityImpl = Visibilities.PUBLIC
    ) {
        private val myDefaultType by lazy { createSimpleType(typeConstructor, nullable) }
        override val storageManager = LockBasedStorageManager.NO_LOCKS
        override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> = emptyList()
        override val underlyingType by lazy { rightHandSideType.getAbbreviation() ?: rightHandSideType }
        override fun getDefaultType() = myDefaultType
        override val classDescriptor get() = expandedType.constructor.declarationDescriptor as ClassDescriptor?
        override val constructors: Collection<TypeAliasConstructorDescriptor> = emptyList()
        override fun substitute(substitutor: TypeSubstitutor) = this
        override val expandedType by lazy { rightHandSideType }
    }

    (rightHandSideType.getAbbreviatedType()?.expandedType ?: rightHandSideType).withAbbreviation(typeAliasDescriptor.defaultType)
}

internal fun mockProperty(
    name: String,
    setterVisibility: Visibility?,
    extensionReceiverType: KotlinType?,
    returnType: KotlinType
): PropertyDescriptor {
    val propertyName = Name.identifier(name)

    val containingDeclaration = object : DeclarationDescriptorImpl(Annotations.EMPTY, Name.special("<fake containing declaration>")) {
        override fun getContainingDeclaration() = error("not supported")
        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D) = error("not supported")
    }

    val propertyDescriptor = PropertyDescriptorImpl.create(
        /*containingDeclaration =*/ containingDeclaration,
        /*annotations =*/ Annotations.EMPTY,
        /*modality =*/ Modality.FINAL,
        /*visibility =*/ Visibilities.PUBLIC,
        /*isVar =*/ setterVisibility != null,
        /*name =*/ propertyName,
        /*kind =*/ CallableMemberDescriptor.Kind.DECLARATION,
        /*source =*/ SourceElement.NO_SOURCE,
        /*lateInit =*/ false,
        /*isConst =*/ false,
        /*isExpect =*/ false,
        /*isActual =*/ false,
        /*isExternal =*/ false,
        /*isDelegated =*/ false
    )

    val extensionReceiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(
        /*owner =*/ propertyDescriptor,
        /*receiverParameterType =*/ extensionReceiverType,
        /*annotations =*/ Annotations.EMPTY
    )

    val dispatchReceiverDescriptor = DescriptorUtils.getDispatchReceiverParameterIfNeeded(containingDeclaration)

    propertyDescriptor.setType(
        /*outType =*/ returnType,
        /*typeParameters =*/ emptyList(),
        /*dispatchReceiverParameter =*/ dispatchReceiverDescriptor,
        /*extensionReceiverParameter =*/ extensionReceiverDescriptor
    )

    val getter = DescriptorFactory.createDefaultGetter(
        /*propertyDescriptor =*/ propertyDescriptor,
        /*annotations =*/ Annotations.EMPTY
    ).apply {
        initialize(null) // use return type from the property descriptor
    }

    val setter = setterVisibility?.let {
        DescriptorFactory.createSetter(
            /*propertyDescriptor =*/ propertyDescriptor,
            /*annotations =*/ Annotations.EMPTY,
            /*parameterAnnotations =*/ Annotations.EMPTY,
            /*isDefault =*/ false,
            /*isExternal =*/ false,
            /*isInline =*/ false,
            /*visibility =*/ setterVisibility,
            /*sourceElement =*/ SourceElement.NO_SOURCE
        )
    }

    propertyDescriptor.initialize(getter, setter)

    return propertyDescriptor
}

//private fun mockTypeParameterType(
//    name: String,
//    containingDeclaration: DeclarationDescriptor,
//    definitelyNotNull: Boolean = false
//): KotlinType {
//    val typeParameterName = Name.identifier(name)
//
//    val typeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
//        /*containingDeclaration =*/ containingDeclaration,
//        /*annotations =*/ Annotations.EMPTY,
//        /*reified =*/ false,
//        /*variance =*/ Variance.INVARIANT,
//        /*name =*/ typeParameterName,
//        /*index =*/ 0
//    )
//
//    val simpleType = createSimpleType(typeParameterDescriptor.typeConstructor, false)
//
//    return if (definitelyNotNull)
//        simpleType.makeSimpleTypeDefinitelyNotNullOrNotNull().also { check(it.isDefinitelyNotNullType) }
//    else
//        simpleType
//}

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

private fun createSimpleType(typeConstructor: TypeConstructor, nullable: Boolean): SimpleType =
    KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        annotations = Annotations.EMPTY,
        constructor = typeConstructor,
        arguments = emptyList(),
        nullable = nullable,
        memberScope = MemberScope.Empty,
        refinedTypeFactory = { null }
    )
