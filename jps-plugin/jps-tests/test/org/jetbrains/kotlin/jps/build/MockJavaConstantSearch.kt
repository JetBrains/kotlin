/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.util.concurrency.FixedFuture
import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File
import java.util.concurrent.Future

/**
 * Mocks Intellij Java constant search.
 * When JPS is run from Intellij, it sends find usages request to IDE (it only searches for references inside Java files).
 *
 * We rely on heuristics instead of precise usages search.
 * A Java file is considered affected if:
 * 1. It contains changed field name as a content substring.
 * 2. Its simple file name is not equal to a field's owner class simple name (to avoid recompiling field's declaration again)
 */
class MockJavaConstantSearch(private val workDir: File) :
    Callbacks.ConstantAffectionResolver {
    override fun request(
        ownerClassName: String,
        fieldName: String,
        accessFlags: Int,
        fieldRemoved: Boolean,
        accessChanged: Boolean
    ): Future<Callbacks.ConstantAffection> {
        fun File.isAffected(): Boolean {
            if (!isJavaFile()) return false

            if (nameWithoutExtension == ownerClassName.substringAfterLast(".")) return false

            val code = readText()
            return code.contains(fieldName)
        }


        val affectedJavaFiles = workDir.walk().filter(File::isAffected).toList()
        return FixedFuture(
            Callbacks.ConstantAffection(
                affectedJavaFiles
            )
        )
    }
}