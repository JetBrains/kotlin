/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import java.util.*

@InternalDokkaApi
public class DocTagParserContext {
    /**
     * exists for resolving `@link element` links, where the referenced
     * PSI element is mapped as DRI
     *
     * only used in the context of parsing to html and then from html to doctag
     */
    private val driMap = mutableMapOf<String, DRI>()

    /**
     * Cache created to make storing entries from kotlin easier.
     *
     * It has to be mutable to allow for adding entries when @inheritDoc resolves to kotlin code,
     * from which we get a DocTags not descriptors.
     */
    private val inheritDocSections = mutableMapOf<String, DocumentationNode>()

    /**
     * @return key of the stored DRI
     */
    public fun store(dri: DRI): String {
        val id = dri.toString()
        driMap[id] = dri
        return id
    }

    /**
     * @return key of the stored documentation node
     */
    public fun store(documentationNode: DocumentationNode): String {
        val id = UUID.randomUUID().toString()
        inheritDocSections[id] = documentationNode
        return id
    }

    public fun getDri(id: String): DRI? = driMap[id]

    public fun getDocumentationNode(id: String): DocumentationNode? = inheritDocSections[id]
}
