/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.api.file.FileTreeElement
import shadow.org.apache.tools.zip.ZipOutputStream
import java.io.File
import shadow.org.apache.commons.io.IOUtils
import shadow.org.apache.tools.zip.ZipEntry
import shadow.org.apache.tools.zip.ZipFile

class CollisionTransformer : Transformer {
    var resolvedConflicts = mutableMapOf<String, File>()
    private val foundConflictsFiles = mutableSetOf<String>()

    override fun getName() = "CollisionTransformer"

    override fun canTransformResource(element: FileTreeElement): Boolean  {
        val result = element.name in resolvedConflicts.keys
        if (result) {
            foundConflictsFiles.add(element.name)
        }
        return result
    }

    override fun transform(context: TransformerContext) {}

    override fun hasTransformedResource(): Boolean {
        return foundConflictsFiles.isNotEmpty()
    }

    override fun modifyOutputStream(jos: ZipOutputStream, preserveFileTimestamps: Boolean) {
        foundConflictsFiles.forEach {
            val entry = ZipEntry(it)
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            jos.putNextEntry(entry)
            val archive = ZipFile(resolvedConflicts[it])
            archive.getInputStream(archive.getEntry(it)).use {
                IOUtils.copyLarge(it, jos)
            }
            jos.closeEntry()
        }
        foundConflictsFiles.clear()
    }
}