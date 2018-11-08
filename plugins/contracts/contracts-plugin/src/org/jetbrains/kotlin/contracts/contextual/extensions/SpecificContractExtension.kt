/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.extensions

import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractDeserializer
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

interface SpecificContractExtension {
    companion object : ProjectExtensionDescriptor<SpecificContractExtension>(
        "org.jetbrains.kotlin.contracts.specificContractExtension",
        SpecificContractExtension::class.java
    ) {
        const val NAME = "org.jetbrains.kotlin.contracts.specificContractExtension"
    }

    fun getFamily(): ContextFamily

    fun getParser(bindingContext: BindingContext, dispatcher: PsiContractVariableParserDispatcher): PsiEffectDeclarationExtractor

    val subpluginContractSerializer: SubpluginContractSerializer
    val subpluginContractDeserializer: SubpluginContractDeserializer
}