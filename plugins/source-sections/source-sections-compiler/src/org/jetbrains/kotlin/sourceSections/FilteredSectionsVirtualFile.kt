/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.sourceSections

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset

class FilteredSectionsVirtualFile(val delegate: VirtualFile, val sectionIds: Collection<String>) : VirtualFile() {

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        delegate.refresh(asynchronous, recursive, postRunnable)
    }

    override fun getLength(): Long = delegate.length
    override fun getFileSystem(): VirtualFileSystem = delegate.fileSystem
    override fun getPath(): String = delegate.path
    override fun isDirectory(): Boolean = delegate.isDirectory
    override fun getTimeStamp(): Long = delegate.timeStamp
    override fun getName(): String = delegate.name

    override fun contentsToByteArray(): ByteArray = filterByteContents(sectionIds, delegate.contentsToByteArray(), delegate.charset)

    override fun getInputStream(): InputStream = ByteArrayInputStream(contentsToByteArray())

    override fun isValid(): Boolean =delegate.isValid
    override fun getParent(): VirtualFile = delegate.parent
    override fun getChildren(): Array<VirtualFile> = delegate.children
    override fun isWritable(): Boolean = delegate.isWritable
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
            delegate.getOutputStream(requestor, newModificationStamp, newTimeStamp)
}

class FilteredSectionsLightVirtualFile(val delegate: LightVirtualFile, val sectionIds: Collection<String>) : LightVirtualFile(delegate.name, delegate.fileType, delegate.content, delegate.charset, delegate.modificationStamp) {
    override fun getContent(): CharSequence = filterStringBuilderContents(StringBuilder(delegate.content), sectionIds)
    override fun contentsToByteArray(): ByteArray = filterByteContents(sectionIds, delegate.contentsToByteArray(), delegate.charset)
}

private fun filterByteContents(sectionIds: Collection<String>, bytes: ByteArray, charset: Charset): ByteArray {
    val content = StringBuilder(charset.decode(ByteBuffer.wrap(bytes)))
    filterStringBuilderContents(content, sectionIds)
    val buffer = charset.encode(CharBuffer.wrap(content))
    return if (buffer.limit() == buffer.capacity()) buffer.array()
        else {
            val res = ByteArray(buffer.limit())
            buffer.get(res)
            res
        }
}

private fun filterStringBuilderContents(content: StringBuilder, sectionIds: Collection<String>): StringBuilder {
    for (i in 0..content.length - 1) {
        if (content[i] == '\r') {
            content[i] = ' '
        }
    }
    var curPos = 0
    val sectionsIter = FilteredSectionsTokensRangeIterator(content, sectionIds)
    for (range in sectionsIter) {
        for (i in curPos..range.start - 1) {
            if (content[i] != '\n') {
                content.setCharAt(i, ' ')
            }
        }
        curPos = range.end
    }
    for (i in curPos..content.length - 1) {
        if (content[i] != '\n') {
            content.setCharAt(i, ' ')
        }
    }
    return content
}


private class TokenRange(val start: Int, val end: Int)

private class FilteredSectionsTokensRangeIterator(script: CharSequence, val sectionIds: Collection<String>) : Iterator<TokenRange> {

    private val lexer = KotlinLexer().apply {
        start(script)
    }

    private var currentRange: TokenRange? = advanceToNextFilteredSection()

    fun advanceToNextFilteredSection(): TokenRange? {

        fun KotlinLexer.skipWhiteSpaceAndComments() {
            while (tokenType in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET) {
                advance()
            }
        }

        var depth = 0
        var sectionStartPos = 0
        var inside = false
        with(lexer) {
            loop@ while (tokenType != null) {
                if (!inside && depth == 0 && tokenType == KtTokens.IDENTIFIER && tokenText in sectionIds) {
                    sectionStartPos = currentPosition.offset
                    advance()
                    skipWhiteSpaceAndComments()
                    inside = (tokenType == KtTokens.LBRACE)
                }
                when (tokenType) {
                    KtTokens.LBRACE -> depth += 1
                    KtTokens.RBRACE -> depth -= 1
                }
                if (inside && depth == 0) {
                    advance()
                    break@loop
                }
                advance()
            }
        }
        return if (lexer.tokenType == null && !inside) null
        else TokenRange(sectionStartPos, lexer.currentPosition.offset)
    }

    override fun hasNext(): Boolean = currentRange != null

    override fun next(): TokenRange {
        val ret = currentRange
        currentRange = advanceToNextFilteredSection()
        return ret!!
    }
}

