/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import java.io.File
import java.net.URL
import kotlin.script.experimental.api.*

fun ScriptSource.getScriptText(): String = when {
    text != null -> text!!
    location != null ->
        location!!.openStream().bufferedReader().readText()
    else -> throw RuntimeException("unable to get text from null script")
}

fun getMergedScriptText(script: ScriptSource, configuration: ScriptCompilationConfiguration?): String {
    val originalScriptText = script.getScriptText()
    val sourceFragments = configuration?.get(ScriptCompilationConfiguration.sourceFragments)
    return if (sourceFragments == null || sourceFragments.isEmpty()) {
        originalScriptText
    } else {
        val sb = StringBuilder(originalScriptText.length)
        var prevFragment: ScriptSourceNamedFragment? = null
        for (fragment in sourceFragments) {
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
