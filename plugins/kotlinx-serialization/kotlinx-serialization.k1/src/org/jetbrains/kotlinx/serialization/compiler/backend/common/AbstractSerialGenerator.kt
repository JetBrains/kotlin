/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.isKSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

abstract class AbstractSerialGenerator(val bindingContext: BindingContext?, val currentDeclaration: ClassDescriptor) {
    private fun getKClassListFromFileAnnotation(annotationFqName: FqName, declarationInFile: DeclarationDescriptor): List<KotlinType> {
        if (bindingContext == null) return emptyList()
        val annotation = AnnotationsUtils
            .getContainingFileAnnotations(bindingContext, declarationInFile)
            .find { it.fqName == annotationFqName }
            ?: return emptyList()

        val typeList = annotation.firstArgument()?.value as? List<*> ?: return emptyList()
        return typeList.filterIsInstance<KClassValue>().map { it.getArgumentType(declarationInFile.module) }
    }

    val contextualKClassListInCurrentFile: Set<KotlinType> by lazy {
        getKClassListFromFileAnnotation(
            SerializationAnnotations.contextualFqName,
            currentDeclaration
        ).plus(
            getKClassListFromFileAnnotation(
                SerializationAnnotations.contextualOnFileFqName,
                currentDeclaration
            )
        ).toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<Pair<ClassDescriptor, Boolean>, ClassDescriptor> by lazy {
        getKClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName, currentDeclaration)
            .associateBy(
                {
                    val kotlinType = it.supertypes().find(::isKSerializer)?.arguments?.firstOrNull()?.type
                    val descriptor = kotlinType.toClassDescriptor
                        ?: throw AssertionError("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                    descriptor to kotlinType!!.isMarkedNullable
                },
                { it.toClassDescriptor!! }
            )
    }

    protected fun ClassDescriptor.getFuncDesc(funcName: String): Sequence<FunctionDescriptor> =
        unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }.asSequence()
            .filterIsInstance<FunctionDescriptor>()
}
