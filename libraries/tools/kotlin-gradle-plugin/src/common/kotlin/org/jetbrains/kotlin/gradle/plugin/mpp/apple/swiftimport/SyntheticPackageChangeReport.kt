/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import java.io.File
import java.security.MessageDigest

internal object SyntheticPackageChangeReport {

    /**
     * A snapshot of a set of files under a single root.
     *
     * Keys are forward-slash, relative-to-root paths (stable across OSes for diagnostic output).
     * Values are SHA-256 digests of each file's content.
     */
    internal data class Snapshot(val root: File, val digests: Map<String, ByteArray>)

    internal data class Changes(
        val added: List<String>,
        val removed: List<String>,
        val modified: List<String>,
    ) {
        val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty() && modified.isEmpty()
    }

    internal fun snapshot(root: File, files: Iterable<File>): Snapshot {
        val rootAbsolute = root.absoluteFile
        val digests = LinkedHashMap<String, ByteArray>()
        // Sort to make snapshot output deterministic regardless of FS iteration order.
        files.sortedBy { it.absolutePath }.forEach { file ->
            val relative = file.absoluteFile.relativeTo(rootAbsolute).path.replace(File.separatorChar, '/')
            val sha = MessageDigest.getInstance("SHA-256")
            sha.update(file.readBytes())
            digests[relative] = sha.digest()
        }
        return Snapshot(rootAbsolute, digests)
    }

    internal fun diff(initial: Snapshot, final: Snapshot): Changes {
        val initialKeys = initial.digests.keys
        val finalKeys = final.digests.keys
        val added = (finalKeys - initialKeys).sorted()
        val removed = (initialKeys - finalKeys).sorted()
        val modified = (initialKeys intersect finalKeys)
            .filter { !initial.digests.getValue(it).contentEquals(final.digests.getValue(it)) }
            .sorted()
        return Changes(added = added, removed = removed, modified = modified)
    }

    /**
     * Renders the changes as `error: ` prefixed lines so they surface as errors in Xcode's build log
     * (Xcode parses lines starting with `error:` and shows them in the issue navigator).
     */
    internal fun render(changes: Changes): String = buildString {
        if (changes.isEmpty) return@buildString
        appendLine("error: Synthetic linkage package files changed during the build:")
        changes.added.forEach { appendLine("error:   + $it (added)") }
        changes.removed.forEach { appendLine("error:   - $it (removed)") }
        changes.modified.forEach { appendLine("error:   ~ $it (modified)") }
    }.trimEnd('\n')
}
