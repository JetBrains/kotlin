/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.createPrimaryConstructorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.computeSealedSubclasses
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinEnum
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class CommonizedClassDescriptor(
    targetComponents: TargetDeclarationsBuilderComponents,
    containingDeclaration: DeclarationDescriptor,
    override val annotations: Annotations,
    name: Name,
    private val kind: ClassKind,
    private val modality: Modality,
    private val visibility: DescriptorVisibility,
    private val isCompanion: Boolean,
    private val isData: Boolean,
    private val isInline: Boolean,
    private val isInner: Boolean,
    isExternal: Boolean,
    private val isExpect: Boolean,
    private val isActual: Boolean,
    cirDeclaredTypeParameters: List<CirTypeParameter>,
    companionObjectName: Name?,
    cirSupertypes: Collection<CirType>
) : ClassDescriptorBase(targetComponents.storageManager, containingDeclaration, name, SourceElement.NO_SOURCE, isExternal) {
    private lateinit var _unsubstitutedMemberScope: CommonizedMemberScope
    private lateinit var constructors: Collection<ClassConstructorDescriptor>
    private var primaryConstructor: ClassConstructorDescriptor? = null

    private val staticScope = if (kind == ClassKind.ENUM_CLASS)
        StaticScopeForKotlinEnum(targetComponents.storageManager, this)
    else
        MemberScope.Empty

    private val typeConstructor = CommonizedClassTypeConstructor(targetComponents, cirSupertypes)
    private val sealedSubclasses = targetComponents.storageManager.createLazyValue { computeSealedSubclasses(this) }

    private val declaredTypeParametersAndTypeParameterResolver = targetComponents.storageManager.createLazyValue {
        val parent = if (isInner) (containingDeclaration as? ClassDescriptor)?.getTypeParameterResolver() else null

        cirDeclaredTypeParameters.buildDescriptorsAndTypeParameterResolver(
            targetComponents,
            parent ?: TypeParameterResolver.EMPTY,
            this
        )
    }

    private val companionObjectDescriptor = targetComponents.storageManager.createNullableLazyValue {
        if (companionObjectName != null)
            unsubstitutedMemberScope.getContributedClassifier(companionObjectName, NoLookupLocation.FOR_ALREADY_TRACKED) as? ClassDescriptor
        else
            null
    }

    override fun getKind() = kind
    override fun getModality() = modality
    override fun getVisibility() = visibility
    override fun isCompanionObject() = isCompanion
    override fun isData() = isData
    override fun isInline() = isInline
    override fun isInner() = isInner
    override fun isExpect() = isExpect
    override fun isActual() = isActual
    override fun isFun() = false // TODO: modifier "fun" should be accessible from here too
    override fun isValue() = false // TODO: modifier "value" should be accessible from here too

    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner): CommonizedMemberScope {
        check(kotlinTypeRefiner == KotlinTypeRefiner.Default) {
            "${kotlinTypeRefiner::class.java} is not supported in ${this::class.java}"
        }
        return _unsubstitutedMemberScope
    }

    override fun getUnsubstitutedMemberScope(): CommonizedMemberScope = super.getUnsubstitutedMemberScope() as CommonizedMemberScope

    fun setUnsubstitutedMemberScope(unsubstitutedMemberScope: CommonizedMemberScope) {
        _unsubstitutedMemberScope = unsubstitutedMemberScope
    }

    override fun getDeclaredTypeParameters() = declaredTypeParametersAndTypeParameterResolver().first
    val typeParameterResolver: TypeParameterResolver get() = declaredTypeParametersAndTypeParameterResolver().second
    override fun getConstructors() = constructors
    override fun getUnsubstitutedPrimaryConstructor() = primaryConstructor
    override fun getStaticScope(): MemberScope = staticScope
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getCompanionObjectDescriptor() = companionObjectDescriptor()
    override fun getSealedSubclasses() = sealedSubclasses()

    fun initialize(constructors: Collection<CommonizedClassConstructorDescriptor>) {
        if (isExpect && kind.isSingleton) {
            check(constructors.isEmpty())

            primaryConstructor = if (kind == ClassKind.ENUM_ENTRY)
                createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply { returnType = getDefaultType() }
            else
                null

            this.constructors = listOfNotNull(primaryConstructor)
        } else {
            constructors.forEach { it.returnType = getDefaultType() }

            primaryConstructor = constructors.firstOrNull { it.isPrimary }
            this.constructors = constructors
        }
    }

    override fun toString() = (if (isExpect) "expect " else if (isActual) "actual " else "") + "class " + name.toString()

    private inner class CommonizedClassTypeConstructor(
        targetComponents: TargetDeclarationsBuilderComponents,
        cirSupertypes: Collection<CirType>
    ) : AbstractClassTypeConstructor(targetComponents.storageManager) {
        private val parameters = targetComponents.storageManager.createLazyValue {
            this@CommonizedClassDescriptor.computeConstructorTypeParameters()
        }

        private val supertypes = targetComponents.storageManager.createLazyValue {
            cirSupertypes.map { it.buildType(targetComponents, this@CommonizedClassDescriptor.typeParameterResolver) }
        }

        override fun getParameters() = parameters()
        override fun computeSupertypes() = supertypes()
        override fun isDenotable() = true
        override fun getDeclarationDescriptor() = this@CommonizedClassDescriptor
        override val supertypeLoopChecker get() = SupertypeLoopChecker.EMPTY
        override fun toString() = declarationDescriptor.fqNameSafe.asString()
    }
}

class CommonizedClassConstructorDescriptor(
    containingDeclaration: ClassDescriptor,
    annotations: Annotations,
    isPrimary: Boolean
) : ClassConstructorDescriptorImpl(
    containingDeclaration,
    null,
    annotations,
    isPrimary,
    CallableMemberDescriptor.Kind.DECLARATION,
    SourceElement.NO_SOURCE
)
