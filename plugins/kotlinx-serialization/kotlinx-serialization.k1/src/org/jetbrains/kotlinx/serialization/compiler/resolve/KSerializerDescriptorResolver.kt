/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.createProjection
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.IMPL_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIALIZER_CLASS_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.typeArgPrefix

object KSerializerDescriptorResolver {

    fun createDeprecatedHiddenAnnotation(module: ModuleDescriptor): AnnotationDescriptor {
        return module.builtIns.createDeprecatedAnnotation(
            "This synthesized declaration should not be used directly",
            level = "HIDDEN"
        )
    }

    fun isSerialInfoImpl(thisDescriptor: ClassDescriptor): Boolean {
        return thisDescriptor.name == IMPL_NAME
                && thisDescriptor.containingDeclaration is LazyClassDescriptor
                && (thisDescriptor.containingDeclaration as ClassDescriptor).isSerialInfoAnnotation
    }

    fun addSerializerFactorySuperType(classDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        if (!classDescriptor.needSerializerFactory()) return
        val serializerFactoryClass =
            classDescriptor.module.getClassFromInternalSerializationPackage("SerializerFactory")
        supertypes.add(KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, serializerFactoryClass, listOf()))
    }

    fun addSerializerSupertypes(classDescriptor: ClassDescriptor, supertypes: MutableList<KotlinType>) {
        val serializableClassDescriptor = getSerializableClassDescriptorBySerializer(classDescriptor) ?: return
        if (supertypes.any(::isKSerializer)) return

        // Add GeneratedSerializer as superinterface for generated $serializer class, and KSerializer to all others
        val fqName = if (classDescriptor.name == SerialEntityNames.SERIALIZER_CLASS_NAME)
            SerialEntityNames.GENERATED_SERIALIZER_FQ
        else
            SerialEntityNames.KSERIALIZER_NAME_FQ
        supertypes.add(classDescriptor.createSerializerTypeFor(serializableClassDescriptor.defaultType, fqName))
    }

    fun addSerializerImplClass(
        thisDescriptor: ClassDescriptor,
        declarationProvider: ClassMemberDeclarationProvider,
        ctx: LazyClassContext
    ): ClassDescriptor {
        val thisDeclaration = declarationProvider.correspondingClassOrObject!!
        val scope = ctx.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)
        val hasTypeParams = thisDescriptor.declaredTypeParameters.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerDescriptor = SyntheticClassOrObjectDescriptor(
            ctx,
            thisDeclaration,
            thisDescriptor, SERIALIZER_CLASS_NAME, thisDescriptor.source,
            scope,
            Modality.FINAL, DescriptorVisibilities.PUBLIC,
            Annotations.create(listOfNotNull(createDeprecatedHiddenAnnotation(thisDescriptor.module), thisDescriptor.jsExportIgnore())),
            DescriptorVisibilities.PRIVATE,
            serializerKind, false
        )
        val typeParameters: List<TypeParameterDescriptor> =
            thisDescriptor.declaredTypeParameters.mapIndexed { index, param ->
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    serializerDescriptor, Annotations.EMPTY, false, Variance.INVARIANT,
                    param.name, index, LockBasedStorageManager.NO_LOCKS
                )
            }
        serializerDescriptor.initialize(typeParameters)
        val secondaryCtors =
            if (!hasTypeParams)
                emptyList()
            else
                listOf(createTypedSerializerConstructorDescriptor(serializerDescriptor, thisDescriptor, typeParameters))
        serializerDescriptor.secondaryConstructors = secondaryCtors
        return serializerDescriptor
    }

    fun generateSerializerProperties(
        thisDescriptor: ClassDescriptor,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        name: Name,
        result: MutableSet<PropertyDescriptor>
    ) {
        val classDescriptor = getSerializableClassDescriptorBySerializer(thisDescriptor) ?: return

        // Do not auto-generate anything for user serializers
        if (!isAllowedToHaveAutoGeneratedSerializerMethods(thisDescriptor, classDescriptor)) return

        if (name == SerialEntityNames.SERIAL_DESC_FIELD_NAME && result.none(thisDescriptor::checkSerializableClassPropertyResult) &&
            fromSupertypes.none { thisDescriptor.checkSerializableClassPropertyResult(it) && it.modality == Modality.FINAL }
        ) {
            result.add(createSerializableClassPropertyDescriptor(thisDescriptor, classDescriptor))
        }
        // don't add local serializer fields if typed constructor is not synthetic
        if (classDescriptor.declaredTypeParameters.isNotEmpty() &&
            findSerializerConstructorForTypeArgumentsSerializers(thisDescriptor, onlyIfSynthetic = true) != null
        ) {
            result.addAll(createLocalSerializersFieldsDescriptor(name, classDescriptor, thisDescriptor))
        }
    }

    fun generateCompanionObjectMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val classDescriptor = getSerializableClassDescriptorByCompanion(thisDescriptor) ?: return

        if (name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && result.none { it.valueParameters.size == classDescriptor.declaredTypeParameters.size }) {
            result.add(createSerializerGetterDescriptor(thisDescriptor, classDescriptor))
        }

        if (thisDescriptor.needSerializerFactory() && name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && result.none { it.valueParameters.size == 1 && it.valueParameters.first().isVararg }) {
            result.add(createSerializerFactoryVarargDescriptor(thisDescriptor))
        }
    }

    fun generateSerializerMethods(
        thisDescriptor: ClassDescriptor,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val classDescriptor = getSerializableClassDescriptorBySerializer(thisDescriptor) ?: return

        // Do not auto-generate anything for user serializers
        if (!isAllowedToHaveAutoGeneratedSerializerMethods(thisDescriptor, classDescriptor)) return

        fun shouldAddSerializerFunction(checkParameters: (FunctionDescriptor) -> Boolean): Boolean {
            // Add 'save' / 'load' iff there is no such declared member AND there is no such final member in supertypes
            return result.none(checkParameters) &&
                    fromSupertypes.none { checkParameters(it) && it.modality == Modality.FINAL }
        }

        val isSave = name == SerialEntityNames.SAVE_NAME &&
                shouldAddSerializerFunction { classDescriptor.checkSaveMethodParameters(it.valueParameters) }
        val isLoad = name == SerialEntityNames.LOAD_NAME &&
                shouldAddSerializerFunction { classDescriptor.checkLoadMethodParameters(it.valueParameters) }
        val isDescriptorGetter = name == SerialEntityNames.CHILD_SERIALIZERS_GETTER &&
                thisDescriptor.typeConstructor.supertypes.any(::isGeneratedKSerializer) &&
                shouldAddSerializerFunction { true /* TODO? */ }

        val isTypeParamsSerializersGetter = name == SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER &&
                thisDescriptor.typeConstructor.supertypes.any(::isGeneratedKSerializer) &&
                classDescriptor.declaredTypeParameters.isNotEmpty() &&
                shouldAddSerializerFunction { true /* TODO? */ }

        if (isSave || isLoad || isDescriptorGetter || isTypeParamsSerializersGetter) {
            result.add(doCreateSerializerFunction(thisDescriptor, name))
        }
    }

    fun generateSerializableClassMethods(thisDescriptor: ClassDescriptor, name: Name, result: MutableCollection<SimpleFunctionDescriptor>) {
        if (thisDescriptor.isInternalSerializable && name == SerialEntityNames.WRITE_SELF_NAME)
            result.add(createWriteSelfFunctionDescriptor(thisDescriptor))
    }

    private fun createSerializableClassPropertyDescriptor(
        thisDescriptor: ClassDescriptor,
        serializableClassDescriptor: ClassDescriptor
    ): PropertyDescriptor {
        val typeParam = listOf(createProjection(serializableClassDescriptor.defaultType, Variance.INVARIANT, null))
        val propertyFromSerializer = thisDescriptor.getGeneratedSerializerDescriptor().getMemberScope(typeParam)
            .getContributedVariables(SerialEntityNames.SERIAL_DESC_FIELD_NAME, NoLookupLocation.FROM_BUILTINS).single()
        val result = doCreateSerializerProperty(
            thisDescriptor,
            SerialEntityNames.SERIAL_DESC_FIELD_NAME,
            propertyFromSerializer.type,
            propertyFromSerializer.typeParameters,
            DescriptorVisibilities.PUBLIC,
            Modality.OPEN // TODO: it was historically OPEN, but I do not see the reasons not to change to FINAL
        )
        result.overriddenDescriptors = listOf(propertyFromSerializer)
        return result
    }

    private fun doCreateSerializerProperty(
        thisDescriptor: ClassDescriptor,
        name: Name,
        type: KotlinType,
        typeParameters: List<TypeParameterDescriptor> = emptyList(),
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        modality: Modality = Modality.FINAL,
        needBackingField: Boolean = false
    ): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(
            thisDescriptor, Annotations.create(listOfNotNull(thisDescriptor.jsExportIgnore())), modality, visibility, false, name,
            CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source, false, false, false, false, false, false
        )

        val extensionReceiverParameter: ReceiverParameterDescriptor? = null // kludge to disambiguate call
        propertyDescriptor.setType(
            type,
            typeParameters,
            thisDescriptor.thisAsReceiverParameter,
            extensionReceiverParameter,
            emptyList()
        )

        val propertyGetter = PropertyGetterDescriptorImpl(
            propertyDescriptor, Annotations.create(listOfNotNull(thisDescriptor.jsExportIgnore())), modality, visibility, false, false, false,
            CallableMemberDescriptor.Kind.SYNTHESIZED, null, thisDescriptor.source
        )
        propertyGetter.initialize(type)

        val backingField = if (needBackingField) FieldDescriptorImpl(Annotations.create(listOfNotNull(thisDescriptor.jsExportIgnore())), propertyDescriptor) else null
        propertyDescriptor.initialize(propertyGetter, null, backingField, null)
        return propertyDescriptor
    }

    private fun doCreateSerializerFunction(
        companionDescriptor: ClassDescriptor,
        name: Name
    ): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            companionDescriptor,
            Annotations.create(listOfNotNull(companionDescriptor.jsExportIgnore())),
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            companionDescriptor.source
        )

        val serializableClassOnImplSite = extractKSerializerArgumentFromImplementation(companionDescriptor)
            ?: throw AssertionError("Serializer does not implement ${SerialEntityNames.KSERIALIZER_CLASS}??")

        val typeParam = listOf(createProjection(serializableClassOnImplSite, Variance.INVARIANT, null))
        val functionFromSerializer = companionDescriptor.getGeneratedSerializerDescriptor().getMemberScope(typeParam)
            .getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS).single()

        functionDescriptor.initialize(
            null,
            companionDescriptor.thisAsReceiverParameter,
            emptyList(),
            functionFromSerializer.typeParameters,
            functionFromSerializer.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
            functionFromSerializer.returnType,
            Modality.OPEN,
            DescriptorVisibilities.PUBLIC
        )

        return functionDescriptor
    }

    fun createValPropertyDescriptor(
        name: Name,
        containingClassDescriptor: ClassDescriptor,
        type: KotlinType,
        visibility: DescriptorVisibility = DescriptorVisibilities.PRIVATE,
        createGetter: Boolean = false
    ): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(
            containingClassDescriptor,
            Annotations.create(listOfNotNull(containingClassDescriptor.jsExportIgnore())), Modality.FINAL, visibility, false, name,
            CallableMemberDescriptor.Kind.SYNTHESIZED, containingClassDescriptor.source, false, false, false, false, false, false
        )
        val extensionReceiverParameter: ReceiverParameterDescriptor? = null // kludge to disambiguate call
        propertyDescriptor.setType(
            type,
            emptyList(), // no need type parameters?
            containingClassDescriptor.thisAsReceiverParameter,
            extensionReceiverParameter,
            emptyList()
        )

        val propertyGetter: PropertyGetterDescriptorImpl? = if (createGetter) {
            PropertyGetterDescriptorImpl(
                propertyDescriptor, Annotations.create(listOfNotNull(containingClassDescriptor.jsExportIgnore())), Modality.FINAL, visibility, false, false, false,
                CallableMemberDescriptor.Kind.SYNTHESIZED, null, containingClassDescriptor.source
            ).apply { initialize(type) }
        } else {
            null
        }

        propertyDescriptor.initialize(propertyGetter, null)

        return propertyDescriptor
    }

    fun createLoadConstructorDescriptor(
        classDescriptor: ClassDescriptor,
        bindingContext: BindingContext,
        metadataPlugin: SerializationDescriptorSerializerPlugin?
    ): ClassConstructorDescriptor {
        if (!classDescriptor.isInternalSerializable) throw IllegalArgumentException()

        val functionDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            classDescriptor,
            Annotations.create(listOfNotNull(createDeprecatedHiddenAnnotation(classDescriptor.module), classDescriptor.jsExportIgnore())),
            false,
            SourceElement.NO_SOURCE
        )

        val markerDesc = classDescriptor.getKSerializerConstructorMarker()
        val markerType = markerDesc.toSimpleType(nullable = true)

        val serializableProperties = bindingContext.serializablePropertiesFor(classDescriptor, metadataPlugin).serializableProperties
        val parameterDescsAsProps = serializableProperties.map { it.descriptor }
        val bitMaskSlotsCount = serializableProperties.bitMaskSlotCount()
        var i = 0
        val consParams = mutableListOf<ValueParameterDescriptor>()
        repeat(bitMaskSlotsCount) {
            consParams.add(
                ValueParameterDescriptorImpl(
                    functionDescriptor, null, i++, Annotations.EMPTY, Name.identifier("seen$i"), functionDescriptor.builtIns.intType, false,
                    false, false, null, functionDescriptor.source
                )
            )
        }
        for (prop in parameterDescsAsProps) {
            consParams.add(
                ValueParameterDescriptorImpl(
                    functionDescriptor, null, i++, prop.annotations, prop.name, prop.type.makeNullableIfNotPrimitive(), false, false,
                    false, null, functionDescriptor.source
                )
            )
        }
        consParams.add(
            ValueParameterDescriptorImpl(
                functionDescriptor, null, i, Annotations.EMPTY, SerialEntityNames.dummyParamName, markerType, false,
                false, false, null, functionDescriptor.source
            )
        )

        functionDescriptor.initialize(
            consParams,
            // load constructor for final classes could be internal, because it can't be used in inheritors
            if (classDescriptor.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC
        )

        functionDescriptor.returnType = classDescriptor.defaultType
        return functionDescriptor
    }

    private fun createTypedSerializerConstructorDescriptor(
        classDescriptor: ClassDescriptor,
        serializableDescriptor: ClassDescriptor,
        typeParameters: List<TypeParameterDescriptor>
    ): ClassConstructorDescriptor {
        val constrDesc = ClassConstructorDescriptorImpl.createSynthesized(
            classDescriptor,
            Annotations.create(listOf(createDeprecatedHiddenAnnotation(classDescriptor.module))),
            false,
            classDescriptor.source
        )
        val serializerClass = classDescriptor.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)
        assert(serializableDescriptor.declaredTypeParameters.size == typeParameters.size)
        val args = List(serializableDescriptor.declaredTypeParameters.size) { index ->
            val pType = KotlinTypeFactory.simpleNotNullType(
                TypeAttributes.Empty,
                serializerClass,
                listOf(TypeProjectionImpl(typeParameters[index].defaultType))
            )

            ValueParameterDescriptorImpl(
                constrDesc, null, index, Annotations.EMPTY, Name.identifier("$typeArgPrefix$index"), pType,
                false, false, false, null, constrDesc.source
            )
        }

        constrDesc.initialize(args, DescriptorVisibilities.PUBLIC, typeParameters)
        constrDesc.returnType = classDescriptor.defaultType
        return constrDesc
    }

    /**
     * Creates free type parameters T0, T1, ... for given serializable class
     * Returns [T0, T1, ...] and [KSerializer<T0>, KSerializer<T1>,...]
     */
    private fun createKSerializerParamsForEachGenericArgument(
        parentFunction: FunctionDescriptor,
        serializableClass: ClassDescriptor,
        actualArgsOffset: Int = 0
    ): Pair<List<TypeParameterDescriptor>, List<ValueParameterDescriptor>> {
        val serializerClass = serializableClass.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)
        val args = mutableListOf<ValueParameterDescriptor>()
        val typeArgs = mutableListOf<TypeParameterDescriptor>()
        var i = 0

        serializableClass.declaredTypeParameters.forEach { _ ->
            val targ = TypeParameterDescriptorImpl.createWithDefaultBound(
                parentFunction, Annotations.EMPTY, false, Variance.INVARIANT,
                Name.identifier("T$i"), i, LockBasedStorageManager.NO_LOCKS
            )

            val pType =
                KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, serializerClass, listOf(TypeProjectionImpl(targ.defaultType)))

            args.add(
                ValueParameterDescriptorImpl(
                    containingDeclaration = parentFunction,
                    original = null,
                    index = actualArgsOffset + i,
                    annotations = Annotations.EMPTY,
                    name = Name.identifier("$typeArgPrefix$i"),
                    outType = pType,
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = parentFunction.source
                )
            )

            typeArgs.add(targ)
            i++
        }

        return typeArgs to args
    }

    private fun createSerializerFactoryVarargDescriptor(thisClass: ClassDescriptor): SimpleFunctionDescriptor {
        val f = SimpleFunctionDescriptorImpl.create(
            thisClass,
            Annotations.create(listOfNotNull(thisClass.jsExportIgnore())),
            SerialEntityNames.SERIALIZER_PROVIDER_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisClass.source
        )
        val serializerClass = thisClass.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)

        val kSerializerStarType =
            KotlinTypeFactory.simpleNotNullType(
                TypeAttributes.Empty,
                serializerClass,
                listOf(StarProjectionImpl(serializerClass.typeConstructor.parameters.first()))
            )

        val varargType = thisClass.builtIns.getArrayType(Variance.OUT_VARIANCE, kSerializerStarType)

        val vararg = ValueParameterDescriptorImpl(
            containingDeclaration = f,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = Name.identifier("typeParamsSerializers"),
            outType = varargType,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = kSerializerStarType,
            source = f.source
        )

        f.initialize(
            null,
            thisClass.thisAsReceiverParameter,
            emptyList(),
            listOf(),
            listOf(vararg),
            kSerializerStarType,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )
        return f
    }

    private fun createSerializerGetterDescriptor(
        thisClass: ClassDescriptor,
        serializableClass: ClassDescriptor
    ): SimpleFunctionDescriptor {
        val f = SimpleFunctionDescriptorImpl.create(
            thisClass,
            Annotations.create(listOfNotNull(thisClass.jsExportIgnore())),
            SerialEntityNames.SERIALIZER_PROVIDER_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisClass.source
        )
        val serializerClass = thisClass.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)

        val (typeArgs, args) = createKSerializerParamsForEachGenericArgument(f, serializableClass)

        val newSerializableType =
            KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, serializableClass, typeArgs.map { TypeProjectionImpl(it.defaultType) })
        val serialReturnType =
            KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, serializerClass, listOf(TypeProjectionImpl(newSerializableType)))

        f.initialize(null, thisClass.thisAsReceiverParameter, emptyList(), typeArgs, args, serialReturnType, Modality.FINAL, DescriptorVisibilities.PUBLIC)
        return f
    }

    private fun KotlinType.makeNullableIfNotPrimitive() =
        if (KotlinBuiltIns.isPrimitiveType(this)) this
        else this.makeNullable()

    fun createWriteSelfFunctionDescriptor(thisClass: ClassDescriptor): SimpleFunctionDescriptor {
        val jvmStaticClass = thisClass.module.findClassAcrossModuleDependencies(JvmStandardClassIds.Annotations.JvmStatic)!!
        val jvmStaticAnnotation = AnnotationDescriptorImpl(jvmStaticClass.defaultType, mapOf(), jvmStaticClass.source)
        val annotations = Annotations.create(listOfNotNull(jvmStaticAnnotation, thisClass.jsExportIgnore()))

        val f = SimpleFunctionDescriptorImpl.create(
            thisClass,
            annotations,
            SerialEntityNames.WRITE_SELF_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisClass.source
        )
        val returnType = f.builtIns.unitType

        val (typeArgs, argsKSer) = createKSerializerParamsForEachGenericArgument(f, thisClass, actualArgsOffset = 3)

        val args = mutableListOf<ValueParameterDescriptor>()

        // object
        val objectType =
            KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, thisClass, typeArgs.map { TypeProjectionImpl(it.defaultType) })
        args.add(
            ValueParameterDescriptorImpl(
                containingDeclaration = f,
                original = null,
                index = 0,
                annotations = Annotations.EMPTY,
                name = Name.identifier("self"),
                outType = objectType,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = f.source
            )
        )

        // encoder
        args.add(
            ValueParameterDescriptorImpl(
                containingDeclaration = f,
                original = null,
                index = 1,
                annotations = Annotations.EMPTY,
                name = Name.identifier("output"),
                outType = thisClass.getClassFromSerializationPackage(SerialEntityNames.STRUCTURE_ENCODER_CLASS).toSimpleType(false),
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = f.source
            )
        )

        //descriptor
        args.add(
            ValueParameterDescriptorImpl(
                containingDeclaration = f,
                original = null,
                index = 2,
                annotations = Annotations.EMPTY,
                name = Name.identifier("serialDesc"),
                outType = thisClass.getClassFromSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS).toSimpleType(false),
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = f.source
            )
        )

        args.addAll(argsKSer)

        f.initialize(
            null,
            null,
            emptyList(),
            typeArgs,
            args,
            returnType,
            Modality.FINAL,
            // write$Self for final classes could be internal, because it can't be called in inheritors
            if (thisClass.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC
        )

        return f
    }

    // create properties typeSerial0, typeSerial1, etc... for storing generic arguments' serializers
    private fun createLocalSerializersFieldsDescriptor(
        name: Name,
        serializableDescriptor: ClassDescriptor,
        serializerDescriptor: ClassDescriptor
    ): List<PropertyDescriptor> {
        if (serializableDescriptor.declaredTypeParameters.isEmpty()) return emptyList()
        val serializerClass = serializableDescriptor.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS)
        val index = name.identifier.removePrefix(typeArgPrefix).toIntOrNull() ?: return emptyList()
        val param = serializerDescriptor.declaredTypeParameters[index]
        val pType =
            KotlinTypeFactory.simpleNotNullType(
                TypeAttributes.Empty,
                serializerClass,
                listOf(TypeProjectionImpl(param.defaultType))
            )
        val desc = doCreateSerializerProperty(serializerDescriptor, Name.identifier("$typeArgPrefix$index"), pType, needBackingField = true)
        return listOf(desc)
    }
}
