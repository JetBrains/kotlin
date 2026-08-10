/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.backend.ast

import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadataImpl
import org.jetbrains.kotlin.js.util.TextOutputImpl

abstract class AbstractNode : JsNode, HasMetadata {
    private data class Internals(
        var commentsBefore: MutableList<JsComment>? = null,
        var commentsAfter: MutableList<JsComment>? = null,
    ) : HasMetadataImpl()

    private var internals: Internals? = null

    private fun getInternals(): Internals {
        internals?.let { return it }
        return Internals().also { internals = it }
    }

    override fun toString(): String {
        val out = TextOutputImpl()
        JsToStringGenerationVisitor(out).accept(this)
        return out.toString()
    }

    override fun getCommentsBeforeNode(): MutableList<JsComment>? = internals?.commentsBefore

    override fun getCommentsAfterNode(): MutableList<JsComment>? = internals?.commentsAfter

    override fun setCommentsBeforeNode(comments: MutableList<JsComment>?) {
        getInternals().commentsBefore = comments
    }

    override fun setCommentsAfterNode(comments: MutableList<JsComment>?) {
        getInternals().commentsAfter = comments
    }

    override fun <T> getData(key: String): T = getInternals().getData(key)

    override fun <T> setData(key: String, value: T) {
        getInternals().setData(key, value)
    }

    override fun hasData(key: String): Boolean = internals?.hasData(key) ?: false

    override fun removeData(key: String) {
        internals?.removeData(key)
    }

    override fun copyMetadataFrom(other: HasMetadata) {
        if (other.getRawMetadata().isNotEmpty()) {
            getInternals().copyMetadataFrom(other)
        }
    }

    override fun getRawMetadata(): Map<String, Any?> = internals?.getRawMetadata() ?: emptyMap()
}

fun <Self, Other> Self.withMetadataFrom(other: Other): Self
        where Self : HasMetadata,
              Self : JsNode,
              Other : HasMetadata,
              Other : JsNode {
    this.copyMetadataFrom(other)
    other.source?.let { source = it }
    commentsBeforeNode = other.commentsBeforeNode
    commentsAfterNode = other.commentsAfterNode
    return this
}
