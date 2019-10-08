/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.core.Commonizer
import org.jetbrains.kotlin.descriptors.commonizer.fqNameWithTypeParameters
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal fun MemberScope.collectMembers(vararg collectors: (DeclarationDescriptor) -> Boolean) =
    getContributedDescriptors().forEach { member ->
        collectors.any { it(member) }
                // each member must be consumed, otherwise - error
                || error("Unhandled member declaration: $member")
    }

@Suppress("FunctionName")
internal inline fun <reified T : DeclarationDescriptor> Collector(
    crossinline typedCollector: (T) -> Unit
): (DeclarationDescriptor) -> Boolean = { candidate ->
    if (candidate is T) {
        typedCollector(candidate)
        true
    } else
        false
}

@Suppress("FunctionName")
internal inline fun <reified T : CallableMemberDescriptor> CallableMemberCollector(
    crossinline typedCollector: (T) -> Unit
): (DeclarationDescriptor) -> Boolean = Collector<T> { candidate ->
    if (candidate.kind.isReal) // omit fake overrides
        typedCollector(candidate)
}

/** Used for approximation of [PropertyDescriptor]s before running concrete [Commonizer]s */
internal data class PropertyApproximationKey(
    val name: Name,
    val extensionReceiverParameter: String?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name,
        property.extensionReceiverParameter?.type?.fqNameWithTypeParameters
    )
}

/** Used for approximation of [SimpleFunctionDescriptor]s before running concrete [Commonizer]s */
internal data class FunctionApproximationKey(
    val name: Name,
    val valueParameters: List<Pair<Name, String>>,
    val extensionReceiverParameter: String?
) {
    constructor(function: SimpleFunctionDescriptor) : this(
        function.name,
        function.valueParameters.map { it.name to it.type.fqNameWithTypeParameters },
        function.extensionReceiverParameter?.type?.fqNameWithTypeParameters
    )
}

/** Used for approximation of [ConstructorDescriptor]s before running concrete [Commonizer]s */
internal data class ConstructorApproximationKey(
    val valueParameters: List<Pair<Name, String>>
) {
    constructor(constructor: ConstructorDescriptor) : this(
        constructor.valueParameters.map { it.name to it.type.fqNameWithTypeParameters }
    )
}