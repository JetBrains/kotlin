package org.jetbrains.idl2k

import java.io.*

class MDNDocumentationCache(val existing: Set<String>, val nonExisting: Set<String>) {

    fun checkInCache(url: String): Boolean? = when (url) {
        in existing -> true
        in nonExisting -> false
        else -> null
    }

    companion object {
        val Empty = MDNDocumentationCache(emptySet(), emptySet())

        fun read(file: File): MDNDocumentationCache {
            val existing = HashSet<String>()
            val nonExisting = HashSet<String>()

            file.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 2) {
                    val url = parts[0]
                    if (parts[1] == "Y") existing.add(url)
                    else if (parts[1] == "N") nonExisting.add(url)
                }
            }

            return MDNDocumentationCache(existing, nonExisting)
        }

        fun writeTo(c: MDNDocumentationCache, file: File) {
            file.bufferedWriter().use {
                (c.existing + c.nonExisting).sorted().joinTo(it, separator = "\n") { "$it|${if (it in c.existing) "Y" else "N"}" }
            }
        }
    }
}