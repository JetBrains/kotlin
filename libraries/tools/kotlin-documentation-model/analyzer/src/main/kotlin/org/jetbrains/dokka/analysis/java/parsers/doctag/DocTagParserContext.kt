/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.kotlin.documentation.KdElementId
import java.util.*

@InternalDokkaApi
internal class DocTagParserContext {
    /**
     * exists for resolving `@link element` links, where the referenced
     * PSI element is mapped to its declaration ID
     *
     * only used in the context of parsing to html and then from html to doctag
     */
    private val declarationIds = mutableSetOf<KdElementId>()

    /**
     * Cache created to make storing entries from kotlin easier.
     *
     * It has to be mutable to allow for adding entries when @inheritDoc resolves to kotlin code,
     * from which we get a DocTags not descriptors.
     */
    private val inheritDocSections = mutableMapOf<String, DocumentationNode>()

    /**
     * @return the key of the stored declaration ID, to be used in the internal HTML protocol
     */
    fun store(declarationId: KdElementId): String {
        declarationIds.add(declarationId)
        return declarationId.value
    }

    /**
     * @return key of the stored documentation node
     */
    fun store(documentationNode: DocumentationNode): String {
        val id = UUID.randomUUID().toString()
        inheritDocSections[id] = documentationNode
        return id
    }

    fun getDeclarationId(id: String): KdElementId? = KdElementId(id).takeIf { it in declarationIds }

    fun getDocumentationNode(id: String): DocumentationNode? = inheritDocSections[id]
}
