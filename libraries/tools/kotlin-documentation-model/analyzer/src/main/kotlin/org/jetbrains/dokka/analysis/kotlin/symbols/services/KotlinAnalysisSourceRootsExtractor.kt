/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.SourceRootsExtractor
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

internal class KotlinAnalysisSourceRootsExtractor : SourceRootsExtractor {
    override fun extract(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<File> {
        return sourceSet.sourceRoots.filter { directory -> directory.isDirectory || directory.extension == "java" }
    }
}
