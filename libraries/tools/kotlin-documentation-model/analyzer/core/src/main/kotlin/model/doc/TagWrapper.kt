package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

sealed class TagWrapper(val root: DocTag) : WithChildren<DocTag> {

    override val children: List<DocTag>
        get() = root.children

    override fun equals(other: Any?): Boolean =
        (
                other != null &&
                        this::class == other::class &&
                        this.root == (other as TagWrapper).root
                )

    override fun hashCode(): Int = root.hashCode()
}

sealed class NamedTagWrapper(root: DocTag, val name: String) : TagWrapper(root) {
    override fun equals(other: Any?): Boolean = super.equals(other) && this.name == (other as NamedTagWrapper).name
    override fun hashCode(): Int = super.hashCode() + name.hashCode()
}

class Description(root: DocTag) : TagWrapper(root)
class Author(root: DocTag) : TagWrapper(root)
class Version(root: DocTag) : TagWrapper(root)
class Since(root: DocTag) : TagWrapper(root)
class See(root: DocTag, name: String, val address: DRI?) : NamedTagWrapper(root, name)
class Param(root: DocTag, name: String) : NamedTagWrapper(root, name)
class Return(root: DocTag) : TagWrapper(root)
class Receiver(root: DocTag) : TagWrapper(root)
class Constructor(root: DocTag) : TagWrapper(root)
class Throws(root: DocTag, name: String) : NamedTagWrapper(root, name)
class Sample(root: DocTag, name: String) : NamedTagWrapper(root, name)
class Deprecated(root: DocTag) : TagWrapper(root)
class Property(root: DocTag, name: String) : NamedTagWrapper(root, name)
class Suppress(root: DocTag) : TagWrapper(root)
class CustomTagWrapper(root: DocTag, name: String) : NamedTagWrapper(root, name)
