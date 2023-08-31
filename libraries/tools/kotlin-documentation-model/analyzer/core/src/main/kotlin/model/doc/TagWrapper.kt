/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model.doc

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.WithChildren

public sealed class TagWrapper : WithChildren<DocTag> {
    public abstract val root: DocTag

    override val children: List<DocTag>
        get() = root.children
}

public sealed class NamedTagWrapper : TagWrapper() {
    public abstract val name: String
}

public data class Description(override val root: DocTag) : TagWrapper()
public data class Author(override val root: DocTag) : TagWrapper()
public data class Version(override val root: DocTag) : TagWrapper()
public data class Since(override val root: DocTag) : TagWrapper()
public data class See(override val root: DocTag, override val name: String, val address: DRI?) : NamedTagWrapper()
public data class Param(override val root: DocTag, override val name: String) : NamedTagWrapper()
public data class Return(override val root: DocTag) : TagWrapper()
public data class Receiver(override val root: DocTag) : TagWrapper()
public data class Constructor(override val root: DocTag) : TagWrapper()
//TODO this naming is confusing since kotlin has Throws annotation
public data class Throws(override val root: DocTag, override val name: String, val exceptionAddress: DRI?) : NamedTagWrapper()
public data class Sample(override val root: DocTag, override val name: String) : NamedTagWrapper()
public data class Deprecated(override val root: DocTag) : TagWrapper()
public data class Property(override val root: DocTag, override val name: String) : NamedTagWrapper()
public data class Suppress(override val root: DocTag) : TagWrapper()
public data class CustomTagWrapper(override val root: DocTag, override val name: String) : NamedTagWrapper()
