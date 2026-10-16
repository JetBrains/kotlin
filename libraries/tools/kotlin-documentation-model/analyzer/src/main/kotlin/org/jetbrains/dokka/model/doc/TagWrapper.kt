/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren
import kotlin.Deprecated

internal sealed class TagWrapper : WithChildren<DocTag> {
    abstract val root: DocTag

    override val children: List<DocTag>
        get() = root.children
}

internal sealed class NamedTagWrapper : TagWrapper() {
    abstract val name: String
}

internal data class Description(override val root: DocTag) : TagWrapper()
internal data class Author(override val root: DocTag) : TagWrapper()
internal data class Version(override val root: DocTag) : TagWrapper()
internal data class Since(override val root: DocTag) : TagWrapper()
internal data class See(override val root: DocTag, override val name: String, val address: DRI?) : NamedTagWrapper()
internal data class Param(override val root: DocTag, override val name: String, val address: DRI? = null) : NamedTagWrapper() {
    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(root: DocTag, name: String) : this(root, name, null)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(root: DocTag, name: String = this.name): Param = Param(root, name, address = null)
}
internal data class Return(override val root: DocTag) : TagWrapper()
internal data class Receiver(override val root: DocTag) : TagWrapper()
internal data class Constructor(override val root: DocTag) : TagWrapper()
//TODO this naming is confusing since kotlin has Throws annotation
internal data class Throws(override val root: DocTag, override val name: String, val exceptionAddress: DRI?) : NamedTagWrapper()
internal data class Sample(override val root: DocTag, override val name: String) : NamedTagWrapper()
internal data class Deprecated(override val root: DocTag) : TagWrapper()
internal data class Property(override val root: DocTag, override val name: String) : NamedTagWrapper()
internal data class Suppress(override val root: DocTag) : TagWrapper()
internal data class CustomTagWrapper(override val root: DocTag, override val name: String) : NamedTagWrapper()
