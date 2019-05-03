/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import java.io.File
import java.io.IOError
import java.io.IOException
import java.net.URL
import kotlin.script.experimental.api.*

// helper function
fun getMergedScriptText(script: SourceCode, configuration: ScriptCompilationConfiguration?): String {
    val originalScriptText = script.text
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

/**
 * The implementation of the SourceCode for a script located in a file
 */
open class FileScriptSource(val file: File, private val preloadedText: String? = null) : ExternalSourceCode {
    override val externalLocation: URL get() = file.toURI().toURL()
    override val text: String by lazy { preloadedText ?: file.readText() }
    override val name: String? get() = file.name
    override val locationId: String? get() = file.path

    override fun equals(other: Any?): Boolean =
        this === other || (other as? FileScriptSource)?.let { file.absolutePath == it.file.absolutePath && textSafe == it.textSafe } == true

    override fun hashCode(): Int = file.absolutePath.hashCode() + textSafe.hashCode() * 23
}

/**
 * The implementation of the SourceCode for a script location pointed by the URL
 */
open class UrlScriptSource(override val externalLocation: URL) : ExternalSourceCode {
    override val text: String by lazy { externalLocation.readText() }
    override val name: String? get() = externalLocation.file
    override val locationId: String? get() = externalLocation.toString()

    override fun equals(other: Any?): Boolean =
        this === other || (other as? UrlScriptSource)?.let { externalLocation == it.externalLocation && textSafe == it.textSafe } == true

    override fun hashCode(): Int = externalLocation.hashCode() + textSafe.hashCode() * 17
}

/**
 * Converts the file into the SourceCode
 */
fun File.toScriptSource(): SourceCode = FileScriptSource(this)

/**
 * The implementation of the ScriptSource for a script in a String
 */
open class StringScriptSource(val source: String, override val name: String? = null) : SourceCode {

    override val text: String get() = source

    override val locationId: String? = null

    override fun equals(other: Any?): Boolean =
        this === other || (other as? StringScriptSource)?.let { text == it.text && name == it.name && locationId == it.locationId } == true

    override fun hashCode(): Int = text.hashCode() + name.hashCode() * 17 + locationId.hashCode() * 23
}

/**
 * Converts the String into the SourceCode
 */
fun String.toScriptSource(name: String? = null): SourceCode = StringScriptSource(this, name)

private val ExternalSourceCode.textSafe: String?
    get() =
        try {
            text
        } catch (e: Throwable) {
            null
        }