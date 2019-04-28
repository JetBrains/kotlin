/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import java.io.Serializable

const val REPL_SNIPPET_FIRST_NO = 1
const val REPL_Snippet_FIRST_GEN = 1

interface ReplSnippetId : Serializable, Comparable<ReplSnippetId> {
    val no: Int
    val generation: Int
}

data class ReplSnippetIdImpl(override val no: Int, override val generation: Int, private val codeHash: Int) : ReplSnippetId, Serializable {

    constructor(no: Int, generation: Int, code: SourceCode) : this(no, generation, code.text.hashCode())

    override fun compareTo(other: ReplSnippetId): Int = (other as? ReplSnippetIdImpl)?.let { otherId ->
        no.compareTo(otherId.no).takeIf { it != 0 }
            ?: generation.compareTo(otherId.generation).takeIf { it != 0 }
            ?: codeHash.compareTo(otherId.codeHash)
    } ?: -1

    companion object {
        private val serialVersionUID: Long = 1L
    }
}
