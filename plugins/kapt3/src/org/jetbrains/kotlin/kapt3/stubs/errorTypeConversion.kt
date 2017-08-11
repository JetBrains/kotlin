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

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.state.updateArgumentModeFromAnnotations
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.kapt3.mapJList
import org.jetbrains.kotlin.kapt3.mapJListIndexed
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError

internal fun convertKtType(
        reference: KtTypeReference?,
        converter: ClassFileToSourceStubConverter,
        shouldBeBoxed: Boolean = false,
        gotTypeElement: KtTypeElement? = null
): JCTree.JCExpression {
    val type = gotTypeElement ?: reference?.typeElement

    if (reference != null) {
        val kotlinType = converter.kaptContext.bindingContext[BindingContext.TYPE, reference]
        if (kotlinType != null && !kotlinType.containsErrorTypes()) {
            val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
            converter.kaptContext.generationState.typeMapper.mapType(kotlinType, signatureWriter,
                    if (shouldBeBoxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT)

            return SignatureParser(converter.treeMaker).parseFieldSignature(
                    signatureWriter.toString(), getDefaultTypeForUnknownType(converter))
        }
    }

    return when (type) {
        is KtUserType -> convertUserType(type, converter, reference)
        is KtNullableType -> {
            // Prevent infinite recursion
            val innerType = type.innerType ?: return getDefaultTypeForUnknownType(converter)
            convertKtType(reference, converter, shouldBeBoxed = true, gotTypeElement = innerType)
        }
        is KtFunctionType -> convertFunctionType(type, converter)
        else -> getDefaultTypeForUnknownType(converter)
    }
}

private fun getDefaultTypeForUnknownType(converter: ClassFileToSourceStubConverter) = converter.treeMaker.FqName("error.NonExistentClass")

private fun convertUserType(type: KtUserType, converter: ClassFileToSourceStubConverter, reference: KtTypeReference?): JCTree.JCExpression {
    val qualifierExpression = type.qualifier?.let { convertUserType(it, converter, null) }
    val referencedName = type.referencedName ?: "error"
    val treeMaker = converter.treeMaker

    val baseExpression = if (qualifierExpression == null) {
        // This could be List<SomeErrorType> or similar. List should be converted to java.util.List in this case.
        val referenceTarget = converter.kaptContext.bindingContext[BindingContext.REFERENCE_TARGET, type.referenceExpression]
        if (referenceTarget is ClassDescriptor) {
            treeMaker.FqName(converter.kaptContext.generationState.typeMapper.mapType(referenceTarget.defaultType).internalName)
        }
        else {
            treeMaker.SimpleName(referencedName)
        }
    } else {
        treeMaker.Select(qualifierExpression, treeMaker.name(referencedName))
    }

    val arguments = type.typeArguments
    if (arguments.isEmpty()) {
        return baseExpression
    }

    val baseType = reference?.let { converter.kaptContext.bindingContext[BindingContext.TYPE, it] }

    return treeMaker.TypeApply(baseExpression, mapJListIndexed(arguments) { index, projection ->
        val argumentType = projection.typeReference?.let { converter.kaptContext.bindingContext[BindingContext.TYPE, it] }
        val typeParameter = argumentType?.constructor?.parameters?.getOrNull(index)
        val argument = baseType?.arguments?.getOrNull(index)

        val variance = if (argument != null && typeParameter != null) {
            val argumentMode = TypeMappingMode.GENERIC_ARGUMENT.updateArgumentModeFromAnnotations(argument.type)
            KotlinTypeMapper.getVarianceForWildcard(typeParameter, argument, argumentMode)
        }
        else {
            null
        }

        convertTypeProjection(projection, variance, converter)
    })
}

private fun convertTypeProjection(type: KtTypeProjection, variance: Variance?, converter: ClassFileToSourceStubConverter): JCTree.JCExpression {
    val reference = type.typeReference
    val treeMaker = converter.treeMaker
    val projectionKind = type.projectionKind

    if (variance === Variance.INVARIANT) {
        return convertKtType(reference, converter, shouldBeBoxed = true)
    }

    return when {
        projectionKind === KtProjectionKind.STAR -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)
        projectionKind === KtProjectionKind.IN || variance === Variance.IN_VARIANCE ->
            treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), convertKtType(reference, converter, shouldBeBoxed = true))
        projectionKind === KtProjectionKind.OUT || variance === Variance.OUT_VARIANCE ->
            treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), convertKtType(reference, converter, shouldBeBoxed = true))
        else -> convertKtType(reference, converter, shouldBeBoxed = true) // invariant
    }
}

private fun convertFunctionType(type: KtFunctionType, converter: ClassFileToSourceStubConverter): JCTree.JCExpression {
    val receiverType = type.receiverTypeReference
    var parameterTypes = mapJList(type.parameters) { convertKtType(it.typeReference, converter) }
    val returnType = convertKtType(type.returnTypeReference, converter)

    if (receiverType != null) {
        parameterTypes = parameterTypes.prepend(convertKtType(receiverType, converter))
    }

    parameterTypes = parameterTypes.append(returnType)

    val treeMaker = converter.treeMaker
    return treeMaker.TypeApply(treeMaker.SimpleName("Function" + (parameterTypes.size - 1)), parameterTypes)
}

fun KotlinType.containsErrorTypes(allowedDepth: Int = 10): Boolean {
    // Need to limit recursion depth in case of complex recursive generics
    if (allowedDepth <= 0) {
        return false
    }

    if (this.isError) return true
    if (this.arguments.any { !it.isStarProjection && it.type.containsErrorTypes(allowedDepth - 1) }) return true
    return false
}
