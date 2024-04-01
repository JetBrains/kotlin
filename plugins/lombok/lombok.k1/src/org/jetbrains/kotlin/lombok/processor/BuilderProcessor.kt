/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.SyntheticJavaClassDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.ENHANCED_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.config.toDescriptorVisibility
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

class BuilderProcessor(private val config: LombokConfig) : Processor {
    companion object {
        private const val BUILDER_DATA = "Lombok.BuilderData"
        private const val TO_BUILDER = "toBuilder"
    }

    @Suppress("IncorrectFormatting") // KTIJ-22227
    override fun contribute(classDescriptor: ClassDescriptor, partsBuilder: SyntheticPartsBuilder, c: LazyJavaResolverContext) {
        if (classDescriptor is SyntheticJavaClassDescriptor) {
            val builderData = classDescriptor.attributes[BUILDER_DATA] as? BuilderData ?: return
            contributeToBuilderClass(classDescriptor, builderData.constructingClass, builderData.builder, partsBuilder)
        } else {
            val builder = Builder.getIfAnnotated(classDescriptor, config) ?: return
            c.contributeToAnnotatedClass(classDescriptor, builder, partsBuilder)
        }
    }

    private fun LazyJavaResolverContext.contributeToAnnotatedClass(classDescriptor: ClassDescriptor, builder: Builder, partsBuilder: SyntheticPartsBuilder) {
        val builderName = Name.identifier(builder.builderClassName.replace("*", classDescriptor.name.asString()))
        val visibility = builder.visibility.toDescriptorVisibility()
        val builderDescriptor = SyntheticJavaClassDescriptor(
            outerContext = this,
            name = builderName,
            outerClass = classDescriptor,
            classKind = ClassKind.CLASS,
            modality = Modality.FINAL,
            visibility = visibility,
            isInner = false,
            isRecord = false,
            annotations = Annotations.EMPTY,
            declaredTypeParameters = emptyList(),
            sealedSubclasses = emptyList(),
            supertypes = listOf(components.module.builtIns.anyType),
            attributes = mapOf(BUILDER_DATA to BuilderData(builder, classDescriptor))
        )
        partsBuilder.addClass(builderDescriptor)
        val builderFunction = classDescriptor.createFunction(
            Name.identifier(builder.builderMethodName),
            valueParameters = emptyList(),
            returnType = builderDescriptor.defaultType,
            modality = Modality.FINAL,
            visibility = visibility,
            receiver = null
        )
        partsBuilder.addStaticFunction(builderFunction)

        if (builder.requiresToBuilder) {
            val toBuilderFunction = classDescriptor.createFunction(
                Name.identifier(TO_BUILDER),
                valueParameters = emptyList(),
                returnType = builderDescriptor.defaultType,
                modality = Modality.FINAL,
                visibility = visibility
            )
            partsBuilder.addMethod(toBuilderFunction)
        }
    }

    private fun contributeToBuilderClass(
        builderClass: ClassDescriptor,
        constructingClass: ClassDescriptor,
        builder: Builder,
        partsBuilder: SyntheticPartsBuilder
    ) {
        val constructor = builderClass.createJavaConstructor(valueParameters = emptyList(), JavaDescriptorVisibilities.PACKAGE_VISIBILITY)
        partsBuilder.addConstructor(constructor)

        val visibility = builder.visibility.toDescriptorVisibility()

        val buildFunction = builderClass.createFunction(
            Name.identifier(builder.buildMethodName),
            valueParameters = emptyList(),
            returnType = constructingClass.defaultType,
            visibility = visibility
        )
        partsBuilder.addMethod(buildFunction)

        for (field in constructingClass.getJavaFields()) {
            createSetterMethod(builder, field, builderClass, partsBuilder)
        }
    }

    private fun createSetterMethod(
        builder: Builder,
        field: PropertyDescriptor,
        builderClass: ClassDescriptor,
        partsBuilder: SyntheticPartsBuilder
    ) {
        Singular.getOrNull(field)?.let { singular ->
            createMethodsForSingularField(builder, singular, field, builderClass, partsBuilder)
            return
        }

        val fieldName = field.name
        val setterName = fieldName.toMethodName(builder)
        val setFunction = builderClass.createFunction(
            name = setterName,
            valueParameters = listOf(LombokValueParameter(fieldName, field.type)),
            returnType = builderClass.defaultType,
            modality = Modality.FINAL,
            visibility = builder.visibility.toDescriptorVisibility()
        )
        partsBuilder.addMethod(setFunction)
    }

    private fun createMethodsForSingularField(
        builder: Builder,
        singular: Singular,
        field: PropertyDescriptor,
        builderClass: ClassDescriptor,
        partsBuilder: SyntheticPartsBuilder
    ) {
        val nameInSingularForm = (singular.singularName ?: field.name.identifier.singularForm)?.let(Name::identifier) ?: return
        val typeName = field.type.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return

        val addMultipleParameterType: KotlinType
        val valueParameters: List<LombokValueParameter>

        when (typeName) {
            in LombokNames.SUPPORTED_COLLECTIONS -> {
                val parameterType = field.parameterType(0) ?: return
                valueParameters = listOf(
                    LombokValueParameter(nameInSingularForm, parameterType)
                )

                val builtIns = field.module.builtIns
                val baseType = when (typeName) {
                    in LombokNames.SUPPORTED_GUAVA_COLLECTIONS -> builtIns.iterable.defaultType
                    else -> builtIns.collection.defaultType
                }

                addMultipleParameterType = baseType.withProperNullability(singular.allowNull)
                    .replace(newArguments = listOf(TypeProjectionImpl(parameterType)),)
            }

            in LombokNames.SUPPORTED_MAPS -> {
                val keyType = field.parameterType(0) ?: return
                val valueType = field.parameterType(1) ?: return
                valueParameters = listOf(
                    LombokValueParameter(Name.identifier("key"), keyType),
                    LombokValueParameter(Name.identifier("value"), valueType),
                )

                addMultipleParameterType = field.module.builtIns.map.defaultType
                    .withProperNullability(singular.allowNull)
                    .replace(newArguments = listOf(TypeProjectionImpl(keyType), TypeProjectionImpl(valueType)))
            }

            in LombokNames.SUPPORTED_TABLES -> {
                val rowKeyType = field.parameterType(0) ?: return
                val columnKeyType = field.parameterType(1) ?: return
                val valueType = field.parameterType(2) ?: return

                val tableDescriptor = field.module.resolveClassByFqName(LombokNames.TABLE, NoLookupLocation.FROM_SYNTHETIC_SCOPE) ?: return

                valueParameters = listOf(
                    LombokValueParameter(Name.identifier("rowKey"), rowKeyType),
                    LombokValueParameter(Name.identifier("columnKey"), columnKeyType),
                    LombokValueParameter(Name.identifier("value"), valueType),
                )

                addMultipleParameterType = tableDescriptor.defaultType
                    .withProperNullability(singular.allowNull)
                    .replace(
                        newArguments = listOf(
                            TypeProjectionImpl(rowKeyType),
                            TypeProjectionImpl(columnKeyType),
                            TypeProjectionImpl(valueType),
                        )
                    )
            }

            else -> return
        }

        val builderType = builderClass.defaultType
        val visibility = builder.visibility.toDescriptorVisibility()

        val addSingleFunction = builderClass.createFunction(
            name = nameInSingularForm.toMethodName(builder),
            valueParameters,
            returnType = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )
        partsBuilder.addMethod(addSingleFunction)

        val addMultipleFunction = builderClass.createFunction(
            name = field.name.toMethodName(builder),
            valueParameters = listOf(LombokValueParameter(field.name, addMultipleParameterType)),
            returnType = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )
        partsBuilder.addMethod(addMultipleFunction)

        val clearFunction = builderClass.createFunction(
            name = Name.identifier("clear${field.name.identifier.capitalize()}"),
            valueParameters = listOf(),
            returnType = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )
        partsBuilder.addMethod(clearFunction)
    }

    private val String.singularForm: String?
        get() = StringUtil.unpluralize(this)

    private class BuilderData(val builder: Builder, val constructingClass: ClassDescriptor)

    private fun PropertyDescriptor.parameterType(index: Int): KotlinType? {
        val type = returnType?.arguments?.getOrNull(index)?.type ?: return null
        return type.replaceAnnotations(
            CompositeAnnotations(
                type.annotations,
                ENHANCED_NULLABILITY_ANNOTATIONS
            )
        )
    }

    private fun KotlinType.withProperNullability(allowNull: Boolean): KotlinType {
        return if (allowNull) makeNullable() else makeNotNullable()
    }

    private fun Name.toMethodName(builder: Builder): Name {
        val prefix = builder.setterPrefix
        return if (prefix.isNullOrBlank()) {
            this
        } else {
            Name.identifier("$prefix${identifier.capitalize()}")
        }
    }
}
