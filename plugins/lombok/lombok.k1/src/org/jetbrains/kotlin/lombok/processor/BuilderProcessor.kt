/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.descriptors.SyntheticJavaClassDescriptor
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.config.toDescriptorVisibility
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name

class BuilderProcessor(private val config: LombokConfig) : Processor {
    companion object {
        private const val BUILDER_DATA = "Lombok.BuilderData"
        private const val TO_BUILDER = "toBuilder"
    }

    context(LazyJavaResolverContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    override fun contribute(classDescriptor: ClassDescriptor, partsBuilder: SyntheticPartsBuilder) {
        if (classDescriptor is SyntheticJavaClassDescriptor) {
            val builderData = classDescriptor.attributes[BUILDER_DATA] as? BuilderData ?: return
            contributeToBuilderClass(classDescriptor, builderData.constructingClass, builderData.builder, partsBuilder)
        } else {
            val builder = Builder.getIfAnnotated(classDescriptor, config) ?: return
            contributeToAnnotatedClass(classDescriptor, builder, partsBuilder)
        }
    }

    context(LazyJavaResolverContext)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun contributeToAnnotatedClass(classDescriptor: ClassDescriptor, builder: Builder, partsBuilder: SyntheticPartsBuilder) {
        val builderName = Name.identifier(builder.builderClassName.replace("*", classDescriptor.name.asString()))
        val visibility = builder.visibility.toDescriptorVisibility()
        val builderDescriptor = SyntheticJavaClassDescriptor(
            outerContext = this@LazyJavaResolverContext,
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
        val prefix = builder.setterPrefix
        val fieldName = field.name
        val setterName = if (prefix.isNullOrBlank()) fieldName else Name.identifier("${prefix}${field.name.asString().capitalize()}")
        val setFunction = builderClass.createFunction(
            name = setterName,
            valueParameters = listOf(LombokValueParameter(fieldName, field.type)),
            returnType = builderClass.defaultType,
            modality = Modality.FINAL,
            visibility = builder.visibility.toDescriptorVisibility()
        )
        partsBuilder.addMethod(setFunction)
    }

    private class BuilderData(val builder: Builder, val constructingClass: ClassDescriptor)
}
