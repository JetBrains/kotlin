/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.artifact

import org.jetbrains.kotlin.pill.artifact.ArtifactElement.*
import org.jetbrains.kotlin.pill.util.PathContext
import org.jetbrains.kotlin.pill.util.xml
import java.io.File

class PArtifact(val artifactName: String, private val outputDir: File, private val contents: Root) {
    fun render(context: PathContext) = xml("component", "name" to "ArtifactManager") {
        xml("artifact", "name" to artifactName) {
            xml("output-path") {
                raw(context(outputDir))
            }

            add(contents.renderRecursively(context))
        }
    }
}