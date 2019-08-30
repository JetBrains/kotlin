/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.isKSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

abstract class AbstractSerialGenerator(val bindingContext: BindingContext, val currentDeclaration: ClassDescriptor) {

    private fun getKClassListFromFileAnnotation(annotationFqName: FqName, declarationInFile: DeclarationDescriptor): List<KotlinType> {
        val annotation = AnnotationsUtils
            .getContainingFileAnnotations(bindingContext, declarationInFile)
            .find { it.fqName == annotationFqName }
            ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val typeList: List<KClassValue> = annotation.firstArgument()?.value as? List<KClassValue> ?: return emptyList()
        return typeList.map { it.getArgumentType(declarationInFile.module) }
    }

    val contextualKClassListInCurrentFile: Set<KotlinType> by lazy {
        getKClassListFromFileAnnotation(
            SerializationAnnotations.contextualFqName,
            currentDeclaration
        ).toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<ClassDescriptor, ClassDescriptor> by lazy {
        getKClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName, currentDeclaration)
            .associateBy(
                {
                    it.supertypes().find(::isKSerializer)?.arguments?.firstOrNull()?.type.toClassDescriptor
                        ?: throw AssertionError("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                },
                { it.toClassDescriptor!! }
            )
    }
}