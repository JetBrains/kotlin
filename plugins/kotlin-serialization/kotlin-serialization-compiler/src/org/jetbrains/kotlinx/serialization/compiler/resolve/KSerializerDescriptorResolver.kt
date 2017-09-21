/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.createProjection
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.util.*

object KSerializerDescriptorResolver {

    val SERIAL_DESC_FIELD = "serialClassDesc"
    val SAVE = "save"
    val LOAD = "load"

    val SERIAL_DESC_FIELD_NAME = Name.identifier(SERIAL_DESC_FIELD)
    val SAVE_NAME = Name.identifier(SAVE)
    val LOAD_NAME = Name.identifier(LOAD)
    val DUMMY_PARAM_NAME = Name.identifier("serializationConstructorMarker")
    val WRITE_SELF_NAME = Name.identifier("write\$Self") //todo: add it as a supertype to synthetic resolving
    val IMPL_NAME = Name.identifier("Impl")


    fun addSerializableSupertypes(classDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        if (classDescriptor.isInternalSerializable && supertypes.none(::isJavaSerializable)) {
            classDescriptor.getJavaSerializableType()?.let { supertypes.add(it) }
        }
    }

    fun isSerialInfoImpl(thisDescriptor: ClassDescriptor): Boolean {
        return thisDescriptor.name == IMPL_NAME
               && thisDescriptor.containingDeclaration is LazyClassDescriptor
               && thisDescriptor.containingDeclaration.annotations.hasAnnotation(serialInfoFqName)
    }

    fun addSerialInfoSuperType(thisDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        if (isSerialInfoImpl(thisDescriptor)) {
            supertypes.add((thisDescriptor.containingDeclaration as LazyClassDescriptor).toSimpleType(false))
        }
    }

    fun addSerializerSupertypes(classDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        val serializableClassDescriptor = getSerializableClassDescriptorBySerializer(classDescriptor) ?: return
        if (supertypes.none(::isKSerializer)) {
            supertypes.add(classDescriptor.getKSerializerType(serializableClassDescriptor.defaultType))
        }
    }

    fun addSerialInfoImplClass(interfaceDesc: ClassDescriptor, declarationProvider: ClassMemberDeclarationProvider, ctx: LazyClassContext): ClassDescriptor {
        val interfaceDecl = declarationProvider.correspondingClassOrObject!!
        val scope = ctx.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)

        return SyntheticClassOrObjectDescriptor(ctx,
                                                interfaceDecl,
                                                interfaceDesc,
                                                IMPL_NAME,
                                                interfaceDesc.source,
                                                scope,
                                                Modality.FINAL,
                                                Visibilities.PUBLIC,
                                                Visibilities.PRIVATE,
                                                ClassKind.CLASS,
                                                false)
    }

    fun generateSerializerProperties(thisDescriptor: ClassDescriptor,
                                     fromSupertypes: ArrayList<PropertyDescriptor>,
                                     name: Name,
                                     result: MutableSet<PropertyDescriptor>) {
       val classDescriptor = getSerializableClassDescriptorBySerializer(thisDescriptor) ?: return
        if (name == SERIAL_DESC_FIELD_NAME && result.none(thisDescriptor::checkSerializableClassPropertyResult) &&
            fromSupertypes.none { thisDescriptor.checkSerializableClassPropertyResult(it) && it.modality == Modality.FINAL} )
                result.add(createSerializableClassPropertyDescriptor(thisDescriptor, classDescriptor))

    }

    fun generateSerializerMethods(thisDescriptor: ClassDescriptor,
                                  fromSupertypes: List<SimpleFunctionDescriptor>,
                                  name: Name,
                                  result: MutableCollection<SimpleFunctionDescriptor>) {
        val classDescriptor = getSerializableClassDescriptorBySerializer(thisDescriptor) ?: return

        fun shouldAddSerializerFunction(checkParameters: (FunctionDescriptor) -> Boolean): Boolean {
            // Add 'save' / 'load' iff there is no such declared member AND there is no such final member in supertypes
            return result.none(checkParameters) &&
                   fromSupertypes.none { checkParameters(it) && it.modality == Modality.FINAL }
        }

        if (name == SAVE_NAME &&
            shouldAddSerializerFunction { classDescriptor.checkSaveMethodParameters(it.valueParameters) }) {
            result.add(createSaveFunctionDescriptor(thisDescriptor, classDescriptor))
        }

        if (name == LOAD_NAME &&
            shouldAddSerializerFunction { classDescriptor.checkLoadMethodParameters(it.valueParameters) }) {
            result.add(createLoadFunctionDescriptor(thisDescriptor, classDescriptor))
        }
    }

    fun createSerializableClassPropertyDescriptor(companionDescriptor: ClassDescriptor, classDescriptor: ClassDescriptor): PropertyDescriptor =
            doCreateSerializerProperty(companionDescriptor, classDescriptor, SERIAL_DESC_FIELD_NAME)

    fun createSaveFunctionDescriptor(companionDescriptor: ClassDescriptor, classDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
            doCreateSerializerFunction(companionDescriptor, classDescriptor, SAVE_NAME)

    fun createLoadFunctionDescriptor(companionDescriptor: ClassDescriptor, classDescriptor: ClassDescriptor): SimpleFunctionDescriptor =
            doCreateSerializerFunction(companionDescriptor, classDescriptor, LOAD_NAME)

    private fun doCreateSerializerProperty(
            companionDescriptor: ClassDescriptor,
            classDescriptor: ClassDescriptor,
            name: Name
    ): PropertyDescriptor {
        val typeParam = listOf(createProjection(classDescriptor.defaultType, Variance.INVARIANT, null))
        val propertyFromSerializer = companionDescriptor.getKSerializerDescriptor().getMemberScope(typeParam)
                .getContributedVariables(name, NoLookupLocation.FROM_BUILTINS).single()

        val propertyDescriptor = PropertyDescriptorImpl.create(
                companionDescriptor, Annotations.EMPTY, Modality.OPEN, Visibilities.PUBLIC, false, name,
                CallableMemberDescriptor.Kind.SYNTHESIZED, companionDescriptor.source, false, false, false, false, false, false
        )

        val extensionReceiverParameter: ReceiverParameterDescriptor? = null // kludge to disambiguate call
        propertyDescriptor.setType(propertyFromSerializer.type,
                                   propertyFromSerializer.typeParameters,
                                   companionDescriptor.thisAsReceiverParameter,
                                   extensionReceiverParameter)

        val propertyGetter = PropertyGetterDescriptorImpl(
                propertyDescriptor, Annotations.EMPTY, Modality.OPEN, Visibilities.PUBLIC, false, false, false,
                CallableMemberDescriptor.Kind.SYNTHESIZED, null, companionDescriptor.source
        )

        propertyGetter.initialize(propertyFromSerializer.type)
        propertyDescriptor.initialize(propertyGetter, null)
        propertyDescriptor.overriddenDescriptors = listOf(propertyFromSerializer)

        return propertyDescriptor
    }

    private fun doCreateSerializerFunction(
            companionDescriptor: ClassDescriptor,
            classDescriptor: ClassDescriptor,
            name: Name
    ): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
                companionDescriptor, Annotations.EMPTY, name, CallableMemberDescriptor.Kind.SYNTHESIZED, companionDescriptor.source
        )

        val typeParam = listOf(createProjection(classDescriptor.defaultType, Variance.INVARIANT, null))
        val functionFromSerializer = companionDescriptor.getKSerializerDescriptor().getMemberScope(typeParam)
                .getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS).single()

        functionDescriptor.initialize(
                null,
                companionDescriptor.thisAsReceiverParameter,
                functionFromSerializer.typeParameters,
                functionFromSerializer.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
                functionFromSerializer.returnType,
                Modality.OPEN,
                Visibilities.PUBLIC
        )

        return functionDescriptor
    }

    fun createLoadConstructorDescriptor(
            classDescriptor: ClassDescriptor,
            bindingContext: BindingContext
    ): ClassConstructorDescriptor {
        if (!classDescriptor.isInternalSerializable) throw IllegalArgumentException()

        val functionDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                classDescriptor,
                Annotations.EMPTY,
                false,
                classDescriptor.source
        )

        val markerDesc = classDescriptor.getKSerializerConstructorMarker()
        val markerType = markerDesc.toSimpleType()

        val parameterDescsAsProps = SerializableProperties(classDescriptor, bindingContext).serializableProperties.map { it.descriptor }
        var i = 0
        val consParams = mutableListOf<ValueParameterDescriptor>()
        consParams.add(ValueParameterDescriptorImpl(functionDescriptor, null, i++, Annotations.EMPTY, Name.identifier("seen"), functionDescriptor.builtIns.intType, false,
                                                    false, false, null, functionDescriptor.source))
        for (prop in parameterDescsAsProps) {
            consParams.add(ValueParameterDescriptorImpl(functionDescriptor, null, i++, prop.annotations, prop.name, prop.type.makeNullableIfNotPrimitive(), false, false,
                                                        false, null, functionDescriptor.source))
        }
        consParams.add(ValueParameterDescriptorImpl(functionDescriptor, null, i++, Annotations.EMPTY, DUMMY_PARAM_NAME, markerType, false,
                                                    false, false, null, functionDescriptor.source))

        functionDescriptor.initialize(
                consParams,
                Visibilities.PUBLIC
        )

        functionDescriptor.returnType = classDescriptor.defaultType
        return functionDescriptor
    }

    private fun KotlinType.makeNullableIfNotPrimitive() =
            if (KotlinBuiltIns.isPrimitiveType(this)) this
            else this.makeNullable()

    fun createWriteSelfFunctionDescriptor(thisClass: ClassDescriptor): FunctionDescriptor {
        val f = SimpleFunctionDescriptorImpl.create(thisClass, Annotations.EMPTY, WRITE_SELF_NAME, CallableMemberDescriptor.Kind.SYNTHESIZED, thisClass.source)
        val returnType = f.builtIns.unitType

        val args = mutableListOf<ValueParameterDescriptor>()
        var i = 0
        args.add(ValueParameterDescriptorImpl(
                f,
                null,
                i++,
                Annotations.EMPTY,
                Name.identifier("output"),
                thisClass.getClassFromSerializationPackage("KOutput").toSimpleType(false),
                false,
                false,
                false,
                null,
                f.source)
        )

        args.add(ValueParameterDescriptorImpl(
                f,
                null,
                i++,
                Annotations.EMPTY,
                Name.identifier("serialDesc"),
                thisClass.getClassFromSerializationPackage("KSerialClassDesc").toSimpleType(false),
                false,
                false,
                false,
                null,
                f.source)
        )

        f.initialize(
                null,
                thisClass.thisAsReceiverParameter,
                emptyList(),
                args,
                returnType,
                Modality.OPEN,
                Visibilities.PUBLIC
        )

        return f
    }

    fun generateDescriptorsForAnnotationImpl(thisDescriptor: ClassDescriptor, name: Name, fromSupertypes: List<PropertyDescriptor>, result: MutableCollection<PropertyDescriptor>) {
        if (isSerialInfoImpl(thisDescriptor)) {
            result.add(fromSupertypes[0].copy(thisDescriptor, Modality.FINAL, Visibilities.PUBLIC, CallableMemberDescriptor.Kind.SYNTHESIZED, true) as PropertyDescriptor)
        }
    }
}
