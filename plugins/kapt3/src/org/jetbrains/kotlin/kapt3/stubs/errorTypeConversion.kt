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
import org.jetbrains.kotlin.kapt3.mapJList
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

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
            converter.typeMapper.mapType(kotlinType, signatureWriter,
                    if (shouldBeBoxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT)

            return SignatureParser(converter.treeMaker).parseFieldSignature(
                    signatureWriter.toString(), getDefaultTypeForUnknownType(converter))
        }
    }

    return when (type) {
        is KtUserType -> convertUserType(type, converter)
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

private fun convertUserType(type: KtUserType, converter: ClassFileToSourceStubConverter): JCTree.JCExpression {
    val qualifierExpression = type.qualifier?.let { convertUserType(it, converter) }
    val referencedName = type.referencedName ?: "error"
    val treeMaker = converter.treeMaker

    val baseExpression = if (qualifierExpression == null) {
        treeMaker.SimpleName(referencedName)
    } else {
        treeMaker.Select(qualifierExpression, treeMaker.name(referencedName))
    }

    val arguments = type.typeArguments
    if (arguments.isEmpty()) {
        return baseExpression
    }

    return treeMaker.TypeApply(baseExpression, mapJList(arguments) { convertTypeProjection(it, converter) })
}

private fun convertTypeProjection(type: KtTypeProjection, converter: ClassFileToSourceStubConverter): JCTree.JCExpression {
    val reference = type.typeReference
    val treeMaker = converter.treeMaker

    return when (type.projectionKind) {
        KtProjectionKind.IN -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER),
                                                  convertKtType(reference, converter, shouldBeBoxed = true))
        KtProjectionKind.OUT -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS),
                                                   convertKtType(reference, converter, shouldBeBoxed = true))
        KtProjectionKind.STAR -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)
        KtProjectionKind.NONE -> return convertKtType(reference, converter, shouldBeBoxed = true)
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