/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtNonPublicApi::class, KaNonPublicApi::class, KaContextParameterApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.findKDoc
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.ir.backend.js.tsexport.*
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.findIsInstanceAnd

@OptIn(KaNonPublicApi::class)
context(_: KaSession)
internal fun <T : ExportedDeclaration> T.addDocumentationAttributes(source: KaDeclarationSymbol?) {
    val kdoc = source?.findKDoc() ?: return

    val isClass = this is ExportedClass
    val sections = mutableListOf<String>()
    val tagsToProcess: MutableList<KDocTag> = kdoc.additionalSections.toMutableList()

    var constructorDescription: List<String>? = null
    val otherConstructorSections = mutableListOf<String>()
    val parameterSections = mutableMapOf<String, MutableList<String>>()
    val functionSections = mutableMapOf<String, MutableList<String>>()

    while (tagsToProcess.isNotEmpty()) {
        val tag = tagsToProcess.removeAt(0)
        tagsToProcess.addAll(tag.children.filterIsInstance<KDocTag>())

        // Skip such sections, since they will be processed as the children tags
        if (tag.name != null && tag is KDocSection) continue

        val jsDocLines = tag.toJsDocTagLines()

        when (val knownTag = tag.knownTag) {
            // Since the @constructor is simply documenting the primary constructor, it makes sense just to move its documentation to the TypeScript constructor declaration
            // Also, we're taking only the first @constructor tag, since in KDoc the following @constrtor tags are ignored
            KDocKnownTag.CONSTRUCTOR if isClass -> {
                if (constructorDescription != null) continue
                constructorDescription = jsDocLines
            }

            // We move the documentation to the dedicated property
            KDocKnownTag.PARAM, KDocKnownTag.PROPERTY -> {
                val firstLine = jsDocLines.firstOrNull() ?: continue

                if (isClass) {
                    otherConstructorSections.addAll(tag.toParamJsDocLines(firstLine, jsDocLines.drop(1)))

                    val propertyName = tag.getSubjectName()
                    val dedicatedProperty = (source as KaClassSymbol).memberScope
                        .run { propertyName?.let { callables(Name.identifier(it)) } }
                        ?.firstIsInstanceOrNull<KaPropertySymbol>()

                    if (dedicatedProperty != null) {
                        val getterName = dedicatedProperty.getter?.getJsNameForOverriddenDeclaration()
                        val setterName = dedicatedProperty.setter?.getJsNameForOverriddenDeclaration()

                        if (getterName != null || setterName != null) {
                            getterName?.let {
                                functionSections.getOrPut(it) { mutableListOf() }
                                    .addAll(jsDocLines)
                            }
                            setterName?.let {
                                functionSections.getOrPut(it) { mutableListOf() }
                                    .addAll(jsDocLines)
                            }
                        } else {
                            parameterSections
                                .getOrPut(dedicatedProperty.getExportedIdentifier()) { mutableListOf() }
                                .addAll(jsDocLines)
                        }
                    }

                } else if (knownTag == KDocKnownTag.PARAM) { // Since we want to avoid @property randomly appear on different declarations (like constructor)
                    sections.addAll(tag.toParamJsDocLines(firstLine, jsDocLines.drop(1)))
                }
            }

            // Other tags do not require any special treatment
            else -> {
                sections.addAll(jsDocLines)
            }
        }
    }

    if (isClass) {
        (constructorDescription.orEmpty() + otherConstructorSections).ifNotEmpty {
            members.firstIsInstanceOrNull<ExportedConstructor>()
                ?.addDocumentationIfThereIsNoOne(this)
        }

        for ((name, lines) in functionSections) {
            // TODO: Handle @JsSymbol
            members.findIsInstanceAnd<ExportedFunction> {
                (it.name as? ExportedMemberName.Identifier)?.value == name
            }?.addDocumentationIfThereIsNoOne(lines)
        }

        for ((name, lines) in parameterSections) {
            for (member in members) {
                if (member !is ExportedMember) continue
                // TODO: Handle @JsSymbol
                if ((member.name as? ExportedMemberName.Identifier)?.value != name) continue
                if (member !is ExportedField && member !is ExportedPropertyGetter) continue

                member.addDocumentationIfThereIsNoOne(lines)
            }
        }
    }

    sections.ifNotEmpty { addDocumentationIfThereIsNoOne(this) }
}

private fun ExportedDeclaration.addDocumentationIfThereIsNoOne(sections: List<String>) {
    if (attributes.firstIsInstanceOrNull<ExportedAttribute.Documentation>() != null) return
    attributes.add(ExportedAttribute.Documentation(sections.toMutableList()))
}

private fun KDocTag.toParamJsDocLines(firstLine: String, restLines: List<String>): List<String> =
    listOf("@param ${getSubjectName()?.plus(" - ") ?: ""}$firstLine") + restLines

private fun KDocTag.toJsDocTagLines(): List<String> {
    val subject = getSubjectName()
    val content = getContent().removeSuffix("\n").lines()
    val contentFirstLine = content.firstOrNull() ?: ""

    val result = mutableListOf<String>()

    // More about the KDoc known tags: https://kotlinlang.org/docs/kotlin-doc.html#block-tags
    // More about the JSDoc known tags: https://jsdoc.app/
    when (knownTag) {
        // Simple mappings
        KDocKnownTag.RETURN -> result.add("@returns $contentFirstLine")
        KDocKnownTag.SEE -> result.add("@see ${subject ?: contentFirstLine}")
        KDocKnownTag.SINCE -> result.add("@since $contentFirstLine")
        KDocKnownTag.AUTHOR -> result.add("@author ${subject ?: contentFirstLine}")
        KDocKnownTag.EXCEPTION, KDocKnownTag.THROWS -> result.add(if (subject != null) "@throws $subject $contentFirstLine" else "@throws $contentFirstLine")
        KDocKnownTag.SUPPRESS -> result.add("@ignore")

        // Special cases

        // Extension receiver is the Kotlin concept, so on the TypeScript level it's just a regular argument
        KDocKnownTag.RECEIVER -> result.add("@param $EXTENSION_RECEIVER_NAME - $contentFirstLine")

        // Since the @constructor is simply documenting the primary constructor, it makes sense just to move its documentation to the TypeScript constructor declaration
        KDocKnownTag.CONSTRUCTOR -> result.add(contentFirstLine)

        // We move the documentation to the dedicated property and its primary constructor param
        KDocKnownTag.PROPERTY -> result.add(contentFirstLine)

        // If it's defined on class we move the documentation to the dedicated property and its primary constructor param
        KDocKnownTag.PARAM -> result.add(contentFirstLine)

        // Since the samples are referred to Kotlin functions to be inlined, it's not relevant for TypeScript
        // to show the usage of a function in Kotlin
        KDocKnownTag.SAMPLE -> {}

        null -> result.add("${getName()?.let { "@$it " } ?: ""}${subject?.let { "$it - " } ?: ""}$contentFirstLine")
    }

    result.addAll(content.drop(1))

    return result
}
