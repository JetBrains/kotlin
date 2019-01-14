/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.extensions.SpecificContractExtension
import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractDeserializer
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.contracts.contextual.transactions.serialization.TransactionContractDeserializer
import org.jetbrains.kotlin.contracts.contextual.transactions.serialization.TransactionContractSerializer
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.resolve.BindingContext

class TransactionContract : SpecificContractExtension {
    override fun getFamily(): ContextFamily = TransactionFamily

    override fun getParser(bindingContext: BindingContext, dispatcher: PsiContractVariableParserDispatcher): PsiEffectDeclarationExtractor =
        PsiTransactionEffectDeclarationExtractor(bindingContext, dispatcher)

    override val subpluginContractSerializer: SubpluginContractSerializer = TransactionContractSerializer()

    override val subpluginContractDeserializer: SubpluginContractDeserializer = TransactionContractDeserializer()
}