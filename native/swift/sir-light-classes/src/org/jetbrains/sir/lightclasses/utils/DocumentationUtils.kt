/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.kdoc.findKDoc
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirInit
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirAbstractClassFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtPropertySymbol
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirInitFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirProtocolFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirVariableFromKtSymbol

internal fun SirDeclaration.translateDocumentation(elements: KDocElements?): String? {
    if (elements == null) return null

    // Properly handle context params, receiver param and ObjCName
    val parameters = mutableListOf<Pair<String, String>>()
    when (this) {
        is SirFunctionFromKtSymbol -> this.contextParameters
        is SirFunctionFromKtPropertySymbol -> this.contextParameters
        else -> null
    }?.let { [contextParam, contextParams] ->
        val contents = contextParams.mapNotNull { param ->
            val content = elements.parameters.firstOrNull { it.first == param.kotlinName }?.second ?: return@mapNotNull null
            val name = param.parameterName ?: param.argumentName ?: return@mapNotNull null
            name to content
        }
        if (contents.isEmpty()) return@let
        val name = contextParam.parameterName!!
        if (contextParams.size == 1) {
            parameters += name to contents.first().second
        } else {
            val content = contents.joinToString(separator = "\n") { [name, content] ->
                "- `$name`: ${content.prependIndent("  ").trim()}"
            }
            parameters += name to content
        }
    }
    when (this) {
        is SirFunction -> this.extensionReceiverParameter
        else -> null
    }?.let { extensionReceiverParam ->
        val content = elements.receiverContent ?: return@let
        val name = extensionReceiverParam.parameterName!!
        parameters += name to content
    }
    when (this) {
        is SirFunction -> this.parameters
        is SirInit -> this.parameters
        else -> emptyList()
    }.forEach { param ->
        val content = elements.parameters.firstOrNull { it.first == param.kotlinName }?.second
            ?: elements.properties.firstOrNull { it.first == param.kotlinName }?.second
            ?: return@forEach
        val name = param.parameterName ?: param.argumentName ?: return@forEach
        parameters += name to content
    }

    return buildString {
        for (content in elements.contents) {
            appendLine(content.trim())
            appendLine()
        }
        if (parameters.isNotEmpty()) {
            appendLine("- Parameters:")
            for ([name, content] in parameters) {
                val content = content.prependIndent("    ").trim().let {
                    if (it.startsWith("-")) "\n    $it" else it
                }
                appendLine("  - $name: $content")
            }
            appendLine()
        }
        if (elements.returnContent != null) {
            appendLine("- Returns: ${elements.returnContent.prependIndent("  ").trim()}")
            appendLine()
        }
        if (elements.throws.isNotEmpty()) {
            appendLine("- Throws:")
            for ([name, content] in elements.throws) {
                appendLine("  - `$name`: ${content.prependIndent("    ").trim()}")
            }
            appendLine()
        }
        for ([name, content] in elements.extensions) {
            appendLine("- $name: ${content.prependIndent("  ").trim()}")
        }
    }.trim().takeIf { it.isNotBlank() }
}

internal fun MutableList<SirAttribute>.addDocumentationVisibility(kdocElements: KDocElements?) {
    if (kdocElements == null || !kdocElements.suppress) return
    add(SirAttribute.Documentation(SirVisibility.INTERNAL))
}

@OptIn(KtNonPublicApi::class)
internal class KDocElements private constructor(
    val constructorContent: String?,
    val properties: List<Pair<String, String>>,
    val receiverContent: String?,
    val parameters: List<Pair<String, String>>,
    val returnContent: String?,
    val throws: List<Pair<String, String>>,
    val extensions: List<Pair<String, String>>,
    val suppress: Boolean = false,
    val contents: List<String>,
) {
    companion object {
        @OptIn(KaNonPublicApi::class, KtNonPublicApi::class)
        context(_: KaSession)
        operator fun invoke(declaration: SirFromKtSymbol<*>): KDocElements? {
            val kdocTags = declaration.ktSymbol.findKDoc()?.additionalSections?.toMutableList<KDocTag>() ?: mutableListOf()

            var constructorContent: String? = null
            val properties = mutableListOf<Pair<String, String>>()
            var receiverContent: String? = null
            val parameters = mutableListOf<Pair<String, String>>()
            var returnContent: String? = null
            val throws = mutableListOf<Pair<String, String>>()
            val extensions = mutableListOf<Pair<String, String>>()
            var suppress = false
            val contents = mutableListOf<String>()

            while (kdocTags.isNotEmpty()) {
                val tag = kdocTags.removeAt(0)
                kdocTags.addAll(0, tag.children.filterIsInstance<KDocTag>())
                if (tag.name != null && tag is KDocSection) continue // processed as the child tags
                when (tag.knownTag) {
                    KDocKnownTag.CONSTRUCTOR -> {
                        if (constructorContent != null) continue
                        constructorContent = tag.getContent()
                    }
                    KDocKnownTag.PROPERTY -> {
                        val name = tag.getSubjectName() ?: continue
                        val content = tag.getContent()
                        properties += name to content
                    }
                    KDocKnownTag.RECEIVER -> {
                        if (receiverContent != null) continue
                        receiverContent = tag.getContent()
                    }
                    KDocKnownTag.PARAM -> {
                        val name = tag.getSubjectName() ?: continue
                        val content = tag.getContent()
                        parameters += name to content
                    }
                    KDocKnownTag.RETURN -> {
                        if (returnContent != null) continue
                        returnContent = tag.getContent()
                    }
                    KDocKnownTag.THROWS, KDocKnownTag.EXCEPTION -> {
                        val name = tag.getSubjectName() ?: continue
                        val content = tag.getContent()
                        throws += name to content
                    }
                    KDocKnownTag.SEE -> tag.getSubjectName()?.let { extensions += "See" to it }
                    KDocKnownTag.AUTHOR -> extensions += "Author" to tag.getContent()
                    KDocKnownTag.SINCE -> extensions += "Since" to tag.getContent()
                    KDocKnownTag.SUPPRESS -> suppress = true
                    KDocKnownTag.SAMPLE -> continue
                    null -> {
                        val tagName = tag.name
                        if (tagName != null) {
                            val key = tag.getSubjectName()?.let { "$tagName $it" } ?: tagName
                            extensions += key to tag.getContent()
                        } else {
                            contents += tag.getContent()
                        }
                    }
                }
            }

            // Additional elements from the class for primary constructor
            if (declaration is SirInitFromKtSymbol && (declaration.ktSymbol as? KaConstructorSymbol)?.isPrimary == true) {
                val classElements = (declaration.parent as? SirAbstractClassFromKtSymbol)?.kdocElements
                if (classElements != null) {
                    properties.addAll(classElements.properties)
                    parameters.addAll(classElements.parameters)
                    throws.addAll(classElements.throws)
                    contents.addIfNotNull(classElements.constructorContent)
                }
            }

            // Additional elements from the class for properties
            if (declaration is SirVariableFromKtSymbol || declaration is SirFunctionFromKtPropertySymbol) {
                val classElements = (declaration.parent as? SirAbstractClassFromKtSymbol)?.kdocElements
                    ?: (declaration.parent as? SirProtocolFromKtSymbol)?.kdocElements
                if (classElements != null) {
                    contents.addAll(classElements.properties.mapNotNull { [key, content] ->
                        content.takeIf { key == declaration.ktSymbol.name?.identifierOrNullIfSpecial }
                    })
                }
            }

            if (constructorContent == null && properties.isEmpty() && receiverContent == null && parameters.isEmpty() &&
                returnContent == null && throws.isEmpty() && extensions.isEmpty() && !suppress && contents.isEmpty()
            ) return null

            return KDocElements(
                constructorContent,
                properties,
                receiverContent,
                parameters,
                returnContent,
                throws,
                extensions,
                suppress,
                contents
            )
        }
    }
}
