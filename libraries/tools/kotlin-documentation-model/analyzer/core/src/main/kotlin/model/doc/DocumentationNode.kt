package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.model.WithChildren

data class DocumentationNode(override val children: List<TagWrapper>): WithChildren<TagWrapper>
