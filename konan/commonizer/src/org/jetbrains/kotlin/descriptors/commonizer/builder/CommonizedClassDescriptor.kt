/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.*
import org.jetbrains.kotlin.resolve.descriptorUtil.computeSealedSubclasses
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinEnum
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class CommonizedClassDescriptor(
    storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    override val annotations: Annotations,
    name: Name,
    private val kind: ClassKind,
    private val modality: Modality,
    private val visibility: Visibility,
    private val isCompanion: Boolean,
    private val isData: Boolean,
    private val isInline: Boolean,
    private val isInner: Boolean,
    isExternal: Boolean,
    private val isExpect: Boolean,
    private val isActual: Boolean,
    companionObjectName: Name?,
    supertypes: Collection<KotlinType>
) : ClassDescriptorBase(storageManager, containingDeclaration, name, SourceElement.NO_SOURCE, isExternal) {
    private lateinit var _unsubstitutedMemberScope: MemberScope
    private lateinit var constructors: Collection<ClassConstructorDescriptor>
    private var primaryConstructor: ClassConstructorDescriptor? = null
    private val staticScope = if (kind == ClassKind.ENUM_CLASS) StaticScopeForKotlinEnum(storageManager, this) else MemberScope.Empty
    private lateinit var declaredTypeParameters: List<TypeParameterDescriptor>
    private val typeConstructor = CommonizedClassTypeConstructor(storageManager, supertypes)
    private val sealedSubclasses = storageManager.createLazyValue { computeSealedSubclasses(this) }

    private val companionObjectDescriptor = storageManager.createNullableLazyValue {
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

    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner): MemberScope {
        check(kotlinTypeRefiner == KotlinTypeRefiner.Default) {
            "${kotlinTypeRefiner::class.java} is not supported in ${this::class.java}"
        }
        return _unsubstitutedMemberScope
    }

    override fun getDeclaredTypeParameters() = declaredTypeParameters
    fun setDeclaredTypeParameters(declaredTypeParameters: List<TypeParameterDescriptor>) {
        this.declaredTypeParameters = declaredTypeParameters
    }

    override fun getConstructors() = constructors
    override fun getUnsubstitutedPrimaryConstructor() = primaryConstructor
    override fun getStaticScope(): MemberScope = staticScope
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getCompanionObjectDescriptor() = companionObjectDescriptor()
    override fun getSealedSubclasses() = sealedSubclasses()

    fun initialize(
        unsubstitutedMemberScope: MemberScope,
        constructors: Collection<CommonizedClassConstructorDescriptor>
    ) {
        _unsubstitutedMemberScope = unsubstitutedMemberScope

        if (isExpect && kind.isSingleton) {
            check(constructors.isEmpty())

            primaryConstructor = createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply { returnType = getDefaultType() }
            this.constructors = listOf(primaryConstructor!!)
        } else {
            constructors.forEach { it.returnType = getDefaultType() }

            primaryConstructor = constructors.firstOrNull { it.isPrimary }
            this.constructors = constructors
        }
    }

    override fun toString() = (if (isExpect) "expect " else if (isActual) "actual " else "") + "class " + name.toString()

    private inner class CommonizedClassTypeConstructor(
        storageManager: StorageManager,
        private val computedSupertypes: Collection<KotlinType>
    ) : AbstractClassTypeConstructor(storageManager) {
        private val parameters = storageManager.createLazyValue {
            this@CommonizedClassDescriptor.computeConstructorTypeParameters()
        }

        override fun getParameters() = parameters()
        override fun computeSupertypes() = computedSupertypes
        override fun isDenotable() = true
        override fun getDeclarationDescriptor() = this@CommonizedClassDescriptor
        override val supertypeLoopChecker get() = SupertypeLoopChecker.EMPTY
    }
}

class CommonizedClassConstructorDescriptor(
    containingDeclaration: ClassDescriptor,
    annotations: Annotations,
    isPrimary: Boolean,
    kind: CallableMemberDescriptor.Kind
) : ClassConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, kind, SourceElement.NO_SOURCE)
