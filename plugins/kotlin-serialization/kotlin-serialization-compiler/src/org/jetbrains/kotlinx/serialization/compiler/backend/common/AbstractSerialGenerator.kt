/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.findAnnotation
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
abstract class AbstractIrGenerator(private val currentClass: IrClass) {
    private fun getClassListFromFileAnnotation(annotationFqName: FqName): List<IrClassSymbol> {
        val annotation = currentClass.fileParent.annotations.findAnnotation(annotationFqName) ?: return emptyList()
        val vararg = annotation.getValueArgument(0) as? IrVararg ?: return emptyList()
        return vararg.elements
            .mapNotNull { (it as? IrClassReference)?.symbol as? IrClassSymbol}
    }

    val contextualKClassListInCurrentFile: Set<IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(
            SerializationAnnotations.contextualFqName,
        ).plus(
            getClassListFromFileAnnotation(
                SerializationAnnotations.contextualOnFileFqName,
            )
        ).toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<Pair<IrClassSymbol, Boolean>, IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName,)
            .associateBy(
                { serializerSymbol ->
                    val kotlinType = (serializerSymbol.owner.superTypes.find(::isKSerializer) as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull
                    val classSymbol = kotlinType?.classOrNull
                        ?: throw AssertionError("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                    classSymbol to kotlinType.isNullable()
                },
                { it }
            )
    }
}

abstract class AbstractSerialGenerator(val bindingContext: BindingContext?, val currentDeclaration: ClassDescriptor) {

    private fun getKClassListFromFileAnnotation(annotationFqName: FqName, declarationInFile: DeclarationDescriptor): List<KotlinType> {
        if (bindingContext == null) return emptyList() // TODO: support @UseSerializers in FIR
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
        unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }.asSequence().filterIsInstance<FunctionDescriptor>()
}
