package org.jetbrains.dokka

import java.net.URI


fun URI.relativeTo(uri: URI): URI {
    // Normalize paths to remove . and .. segments
    val base = uri.normalize()
    val child = this.normalize()

    fun StringBuilder.appendRelativePath() {
        // Split paths into segments
        var bParts = base.path.split('/').dropLastWhile { it.isEmpty() }
        val cParts = child.path.split('/').dropLastWhile { it.isEmpty() }

        // Discard trailing segment of base path
        if (bParts.isNotEmpty() && !base.path.endsWith("/")) {
            bParts = bParts.dropLast(1)
        }

        // Compute common prefix
        val commonPartsSize = bParts.zip(cParts).takeWhile { (basePart, childPart) -> basePart == childPart }.count()
        bParts.drop(commonPartsSize).joinTo(this, separator = "") { "../" }
        cParts.drop(commonPartsSize).joinTo(this, separator = "/")
    }

    return URI.create(buildString {
        if (base.path != child.path) {
            appendRelativePath()
        }
        child.rawQuery?.let {
            append("?")
            append(it)
        }
        child.rawFragment?.let {
            append("#")
            append(it)
        }
    })
}