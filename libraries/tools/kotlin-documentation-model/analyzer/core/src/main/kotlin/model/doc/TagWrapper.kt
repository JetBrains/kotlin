package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

sealed class TagWrapper : WithChildren<DocTag> {
    abstract val root: DocTag
    override val children: List<DocTag>
        get() = root.children
}

sealed class NamedTagWrapper : TagWrapper() {
    abstract val name: String
}

data class Description(override val root: DocTag) : TagWrapper()
data class Author(override val root: DocTag) : TagWrapper()
data class Version(override val root: DocTag) : TagWrapper()
data class Since(override val root: DocTag) : TagWrapper()
data class See(override val root: DocTag, override val name: String, val address: DRI?) : NamedTagWrapper()
data class Param(override val root: DocTag, override val name: String) : NamedTagWrapper()
data class Return(override val root: DocTag) : TagWrapper()
data class Receiver(override val root: DocTag) : TagWrapper()
data class Constructor(override val root: DocTag) : TagWrapper()
//TODO this naming is confusing since kotlin has Throws annotation
data class Throws(override val root: DocTag, override val name: String, val exceptionAddress: DRI?) : NamedTagWrapper()
data class Sample(override val root: DocTag, override val name: String) : NamedTagWrapper()
data class Deprecated(override val root: DocTag) : TagWrapper()
data class Property(override val root: DocTag, override val name: String) : NamedTagWrapper()
data class Suppress(override val root: DocTag) : TagWrapper()
data class CustomTagWrapper(override val root: DocTag, override val name: String) : NamedTagWrapper()
