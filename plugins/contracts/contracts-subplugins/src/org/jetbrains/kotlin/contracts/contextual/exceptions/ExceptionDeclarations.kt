/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

internal class ExceptionProviderDeclaration(val exceptionType: KotlinType) : ProviderDeclaration {
    override val family = ExceptionFamily
    override val references: List<ContractDescriptionValue> = emptyList()

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ExceptionContextProvider =
        ExceptionContextProvider(exceptionType)

    override fun toString(): String {
        return "Catches $exceptionType"
    }
}

internal class ExceptionVerifierDeclaration(val exceptionType: KotlinType) : VerifierDeclaration {
    override val family = ExceptionFamily
    override val references: List<ContractDescriptionValue> = emptyList()

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ExceptionVerifier =
        ExceptionVerifier(exceptionType, sourceElement)

    override fun toString(): String {
        return "Catches $exceptionType"
    }
}