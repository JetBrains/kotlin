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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kapt3.mapJList
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.resolve.DescriptorUtils.isAnonymousObject
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace

class AnonymousTypeHandler(private val converter: ClassFileToSourceStubConverter) {
    fun <T : JCTree.JCExpression?> getNonAnonymousType(descriptor: DeclarationDescriptor?, f: () -> T): T {
        val classType = when (descriptor) {
            is ClassDescriptor -> descriptor.defaultType
            is CallableDescriptor -> descriptor.returnType
            else -> null
        } ?: return f()

        return getNonAnonymousType(classType, f)
    }

    fun <T : JCTree.JCExpression?> getNonAnonymousType(type: KotlinType, f: () -> T): T {
        if (!checkIfAnonymousRecursively(type)) return f()

        @Suppress("UNCHECKED_CAST")
        return convertKotlinType(convertPossiblyAnonymousType(type)) as T
    }

    private fun checkIfAnonymousRecursively(type: KotlinType): Boolean {
        val declaration = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
        if (isAnonymousObject(declaration)) return true
        return type.arguments.any {
            if (it.isStarProjection) return@any false
            checkIfAnonymousRecursively(it.type)
        }
    }

    private fun convertPossiblyAnonymousType(type: KotlinType): KotlinType {
        val declaration = type.constructor.declarationDescriptor as? ClassDescriptor ?: return type

        val actualType = when {
            isAnonymousObject(declaration) -> findMostSuitableParentForAnonymousType(declaration)
            else -> type
        }

        if (actualType.arguments.isEmpty()) return actualType

        val arguments = actualType.arguments.map { typeArg ->
            if (typeArg.isStarProjection) return@map typeArg
            TypeProjectionImpl(typeArg.projectionKind, convertPossiblyAnonymousType(typeArg.type))
        }

        return actualType.replace(arguments)
    }

    private fun findMostSuitableParentForAnonymousType(descriptor: ClassDescriptor): KotlinType {
        descriptor.getSuperClassNotAny()?.let { return it.defaultType }

        val sortedSuperTypes = descriptor.typeConstructor.supertypes
                .sortedBy { it.constructor.declarationDescriptor?.name?.asString() ?: "" }

        for (candidate in sortedSuperTypes) {
            if (!candidate.isAnyOrNullableAny()) return candidate
        }

        return descriptor.builtIns.anyType
    }

    private fun convertKotlinType(type: KotlinType): JCTree.JCExpression {
        val typeMapper = converter.kaptContext.generationState.typeMapper

        val treeMaker = converter.treeMaker
        val selfType = treeMaker.Type(typeMapper.mapType(type))
        if (type.arguments.isEmpty()) return selfType

        return treeMaker.TypeApply(selfType, mapJList(type.arguments) { projection ->
            if (projection.isStarProjection) return@mapJList treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)

            val renderedArg = convertKotlinType(projection.type)
            when (projection.projectionKind) {
                Variance.INVARIANT -> renderedArg
                Variance.OUT_VARIANCE -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), renderedArg)
                Variance.IN_VARIANCE -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), renderedArg)
            }
        })
    }
}