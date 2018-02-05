/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import kotlin.script.experimental.api.ScriptSource
import kotlin.script.experimental.api.ScriptSourceFragments
import kotlin.script.experimental.api.ScriptSourceNamedFragment
import java.io.File
import java.net.URL

fun ScriptSourceFragments.isWholeFile(): Boolean = fragments?.isEmpty() ?: true

fun ScriptSource.getScriptText(): String = when {
    text != null -> text!!
    location != null ->
        location!!.openStream().bufferedReader().readText()
    else -> throw RuntimeException("unable to get text from null script")
}

fun ScriptSourceFragments.getMergedScriptText(): String {
    val originalScriptText = originalSource.getScriptText()
    return if (isWholeFile()) {
        originalScriptText
    } else {
        val sb = StringBuilder(originalScriptText.length)
        var prevFragment: ScriptSourceNamedFragment? = null
        for (fragment in fragments!!) {
            val fragmentStartPos = fragment.range.start.absolutePos
            val fragmentEndPos = fragment.range.end.absolutePos
            if (fragmentStartPos == null || fragmentEndPos == null)
                throw RuntimeException("Script fragments require absolute positions (received: $fragment)")
            val curPos = if (prevFragment == null) 0 else prevFragment.range.end.absolutePos!!
            if (prevFragment != null && prevFragment.range.end.absolutePos!! > fragmentStartPos) throw RuntimeException("Unsorted or overlapping fragments: previous: $prevFragment, current: $fragment")
            if (curPos < fragmentStartPos) {
                sb.append(
                    originalScriptText.subSequence(
                        curPos,
                        fragmentStartPos
                    ).map { if (it == '\r' || it == '\n') it else ' ' }) // preserving lines layout
            }
            sb.append(originalScriptText.subSequence(fragmentStartPos, fragmentEndPos))
            prevFragment = fragment
        }
        sb.toString()
    }
}

open class FileScriptSource(val file: File) : ScriptSource {
    override val location: URL? get() = file.toURI().toURL()
    override val text: String? get() = null
}

fun File.toScriptSource(): ScriptSource = FileScriptSource(this)

open class StringScriptSource(val source: String) : ScriptSource {
    override val location: URL? get() = null
    override val text: String? get() = source
}

fun String.toScriptSource(): ScriptSource = StringScriptSource(this)
