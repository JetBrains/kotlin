/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance

class CommonizedTypeParameterDescriptor(
    private val targetComponents: TargetDeclarationsBuilderComponents,
    private val typeParameterResolver: TypeParameterResolver,
    containingDeclaration: DeclarationDescriptor,
    override val annotations: Annotations,
    name: Name,
    variance: Variance,
    isReified: Boolean,
    index: Int,
    private val cirUpperBounds: List<CirType>
) : AbstractLazyTypeParameterDescriptor(
    targetComponents.storageManager,
    containingDeclaration,
    name,
    variance,
    isReified,
    index,
    SourceElement.NO_SOURCE,
    SupertypeLoopChecker.EMPTY
) {
    override fun resolveUpperBounds(): List<UnwrappedType> {
        return if (cirUpperBounds.isEmpty())
            listOf(targetComponents.builtIns.defaultBound)
        else
            cirUpperBounds.map { it.buildType(targetComponents, typeParameterResolver) }
    }

    override fun reportSupertypeLoopError(type: KotlinType) =
        error("There should be no cycles for commonized type parameters, but found for: $type in $this")
}
