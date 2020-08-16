/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.builtins.KotlinBuiltInsNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.nj2k.inference.common.State
import org.jetbrains.kotlin.nj2k.inference.common.StateUpdater
import org.jetbrains.kotlin.nj2k.inference.common.TypeElementBasedTypeVariable
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class MutabilityStateUpdater : StateUpdater() {
    override fun TypeElementBasedTypeVariable.updateState() = when (state) {
        State.LOWER -> changeState(toMutable = true)
        State.UPPER -> changeState(toMutable = false)
        else -> Unit
    }

    companion object {
        fun TypeElementBasedTypeVariable.changeState(toMutable: Boolean) {
            changeState(typeElement.typeElement, typeElement.type, toMutable)
        }

        fun changeState(typeElement: KtTypeElement, type: KotlinType, toMutable: Boolean) {
            if (type !is SimpleType) return
            val userTypeElement = when (typeElement) {
                is KtUserType -> typeElement
                is KtNullableType -> typeElement.innerType as? KtUserType
                else -> null
            } ?: return
            val initialFqName = type.constructor
                .declarationDescriptor
                .safeAs<ClassDescriptor>()
                ?.fqNameOrNull()
                ?: return
            val newFqName = when {
                !toMutable && initialFqName in mutableToImmutable -> mutableToImmutable[initialFqName]
                toMutable && initialFqName in immutableToMutable -> immutableToMutable[initialFqName]
                else -> null
            } ?: return
            val factory = KtPsiFactory(typeElement)
            userTypeElement.referenceExpression?.replace(factory.createSimpleName(newFqName.shortName().identifier))
            userTypeElement.qualifier.safeAs<KtUserType>()?.replace(factory.createType(newFqName.parent().asString()).typeElement ?: return)
        }

        val mutableToImmutable = mapOf(
            KotlinBuiltInsNames.FqNames.mutableIterator to KotlinBuiltInsNames.FqNames.iterator,
            KotlinBuiltInsNames.FqNames.mutableCollection to KotlinBuiltInsNames.FqNames.collection,
            KotlinBuiltInsNames.FqNames.mutableList to KotlinBuiltInsNames.FqNames.list,
            KotlinBuiltInsNames.FqNames.mutableListIterator to KotlinBuiltInsNames.FqNames.listIterator,
            KotlinBuiltInsNames.FqNames.mutableSet to KotlinBuiltInsNames.FqNames.set,
            KotlinBuiltInsNames.FqNames.mutableMap to KotlinBuiltInsNames.FqNames.map,
            KotlinBuiltInsNames.FqNames.mutableMapEntry to KotlinBuiltInsNames.FqNames.mapEntry
        )

        val immutableToMutable = mutableToImmutable.map { (key, value) -> value to key }.toMap()
    }
}
