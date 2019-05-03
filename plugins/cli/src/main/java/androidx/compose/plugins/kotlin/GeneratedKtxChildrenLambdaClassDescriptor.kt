/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.ClassTypeConstructorImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer

open class GeneratedKtxChildrenLambdaClassDescriptor(
    private val module: ModuleDescriptor,
    private val containingDeclaration: DeclarationDescriptor,
    private val variablesToCapture: List<KotlinType>,
    functionType: KotlinType,
    private val parameters: List<ValueParameterDescriptor>
) : ClassDescriptor {

    companion object {
        private var index = 1
    }

    private val internalName by lazy {
        Name.identifier("KTX_CHILD_${index++}")
    }

    override fun isInline(): Boolean = false
    override fun getName() = internalName
    override fun isExpect(): Boolean = false
    override fun isActual(): Boolean = false
    private val modality = Modality.FINAL
    private val kind = ClassKind.CLASS
    private val sourceElement = SourceElement.NO_SOURCE
    override val annotations = Annotations.EMPTY
    private lateinit var typeConstructor: TypeConstructor
    private lateinit var supertypes: Collection<KotlinType>
    private lateinit var defaultType: SimpleType
    private lateinit var declaredTypeParameters: List<TypeParameterDescriptor>
    private var unsubstitutedPrimaryConstructor: ClassConstructorDescriptor? = null

    init {
        initialize(emptyList(), listOf(functionType))
    }

    val capturedAccessesAsProperties by lazy {
        variablesToCapture.mapIndexed { index, type ->
            PropertyDescriptorImpl.create(
                this,
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PRIVATE,
                false,
                Name.identifier("p$index"),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE,
                false,
                false,
                true,
                true,
                false,
                false
            ).apply {
                setType(
                    type,
                    emptyList<TypeParameterDescriptor>(),
                    thisAsReceiverParameter,
                    null as ReceiverParameterDescriptor?
                )
                initialize(
                    PropertyGetterDescriptorImpl(
                        this,
                        Annotations.EMPTY,
                        Modality.FINAL,
                        Visibilities.PRIVATE,
                        false,
                        false,
                        false,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        null,
                        SourceElement.NO_SOURCE
                    ).apply {
                        initialize(type)
                    },
                    PropertySetterDescriptorImpl(
                        this,
                        Annotations.EMPTY,
                        Modality.FINAL,
                        Visibilities.PRIVATE,
                        false,
                        false,
                        false,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        null,
                        SourceElement.NO_SOURCE
                    ).apply {
                        initializeDefault()
                    }
                )
            }
        }
    }

    val invokeDescriptor by lazy {
        val functionNInterface =
            module.findClassAcrossModuleDependencies(
                ClassId.topLevel(FqName("kotlin.jvm.functions.Function" + parameters.size))
            )!!
        val overridenMethod =
            functionNInterface.unsubstitutedMemberScope.getContributedFunctions(
                Name.identifier("invoke"),
                NoLookupLocation.FROM_BACKEND
            ).single()
        val newMethod = SimpleFunctionDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            Name.identifier("invoke"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )
        newMethod.setSingleOverridden(overridenMethod)

        val parameters = parameters.map {
            ValueParameterDescriptorImpl(
                newMethod,
                it,
                it.index,
                it.annotations,
                it.name,
                it.type,
                false,
                false,
                false,
                null,
                it.source
            )
        }

        newMethod.initialize(
            null,
            this.thisAsReceiverParameter,
            emptyList(),
            parameters,
            builtIns.unitType,
            Modality.OPEN,
            Visibilities.PUBLIC
        )
        newMethod
    }

    private val thisAsReceiverParameter = LazyClassReceiverParameterDescriptor(this)

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        supertypes: Collection<KotlinType>
    ) {
        this.declaredTypeParameters = declaredTypeParameters
        this.supertypes = supertypes
        this.typeConstructor = ClassTypeConstructorImpl(
            this,
            declaredTypeParameters,
            supertypes,
            LockBasedStorageManager.NO_LOCKS
        )
        this.defaultType =
            TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getConstructors(): Collection<ClassConstructorDescriptor> =
        listOf(getUnsubstitutedPrimaryConstructor())
    override fun getContainingDeclaration() = containingDeclaration
    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = declaredTypeParameters
    override fun getKind(): ClassKind = kind
    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

    fun genScope(): MemberScope {

        return object : MemberScope {

            override fun getFunctionNames(): Set<Name> = setOf(Name.identifier("invoke"))

            override fun getVariableNames(): Set<Name> = emptySet()

            override fun getClassifierNames(): Set<Name>? = null

            override fun getContributedClassifier(
                name: Name,
                location: LookupLocation
            ): ClassifierDescriptor? = null

            override fun getContributedDescriptors(
                kindFilter: DescriptorKindFilter,
                nameFilter: (Name) -> Boolean
            ): Collection<DeclarationDescriptor> {
                return emptyList()
            }

            override fun getContributedVariables(
                name: Name,
                location: LookupLocation
            ): Collection<PropertyDescriptor> {
                return capturedAccessesAsProperties
            }

            override fun getContributedFunctions(
                name: Name,
                location: LookupLocation
            ): Collection<SimpleFunctionDescriptor> {
                if (name.identifier == "invoke") {
                    return listOf(invokeDescriptor)
                }
                return emptyList()
            }

            override fun printScopeStructure(p: Printer) {
                p.println(this::class.java.simpleName)
            }
        }
    }

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope =
        genScope()
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = genScope()
    override fun getStaticScope(): MemberScope = genScope()
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = genScope()
    override fun getUnsubstitutedMemberScope(): MemberScope = genScope()

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor {
        unsubstitutedPrimaryConstructor?.let { return it }
        val constructor = ClassConstructorDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            false,
            SourceElement.NO_SOURCE
        )

        val parameters = variablesToCapture.mapIndexed { index, parameterType ->
            ValueParameterDescriptorImpl(
                constructor,
                null, index, Annotations.EMPTY,
                Name.identifier("p$index"),
                parameterType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE
            )
        }

        constructor.initialize(
            parameters,
            Visibilities.PUBLIC
        )

        constructor.apply {
            returnType = containingDeclaration.defaultType
        }

        unsubstitutedPrimaryConstructor = constructor

        return constructor
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor =
        error("Class $this can't be substituted")
    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor = thisAsReceiverParameter
    override fun getModality(): Modality = modality
    override fun getOriginal(): ClassDescriptor = this
    override fun getVisibility(): Visibility = Visibilities.PUBLIC
    override fun getSource(): SourceElement = sourceElement
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getDefaultType(): SimpleType = defaultType
    override fun isCompanionObject(): Boolean = false
    override fun isData(): Boolean = false
    override fun isInner(): Boolean = false
    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(
        visitor: DeclarationDescriptorVisitor<R, D>,
        data: D
    ): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun toString(): String = "GeneratedChildrenLambdaClassDescriptor($fqNameUnsafe)"
}
