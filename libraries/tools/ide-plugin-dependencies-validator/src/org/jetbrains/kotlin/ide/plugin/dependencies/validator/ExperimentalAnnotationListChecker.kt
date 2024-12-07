/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name

object ExperimentalAnnotationListChecker {
    val nonJvmSubdirsOfStdlibDir = listOf("js", "wasm", "native-wasm", "test", "jvm-minimal-for-test", "samples")
    fun checkNoMissingAnnotations(stdlibPathString: String) {
        val stdlibPath = Paths.get(stdlibPathString)
        require(stdlibPath.exists()) { "Stdlib path $stdlibPathString doesn't exist" }

        val actual = buildSet {
            Files.newDirectoryStream(stdlibPath).use { stream ->
                stream.forEach { path ->
                    if (path.name in nonJvmSubdirsOfStdlibDir) return@forEach
                    addAll(collectExperimentalAnnotationsByStdlib(path))
                }
            }
        } - allowedAnnotations

        val expected = ExperimentalAnnotations.experimentalAnnotations

        run {
            val missing = expected - actual
            if (missing.isNotEmpty()) {
                error("Missing annotations in the experimental annotation list: $missing")
            }
        }
        run {
            val new = actual - expected
            if (new.isNotEmpty()) {
                error("New annotations in the experimental annotation list: $new")
            }
        }
    }

    fun collectExperimentalAnnotationsByStdlib(stdlibPath: Path): Set<String> = buildSet {
        forEachKtFileInDirectory(stdlibPath) { ktFile, _ ->
            ktFile.collectDescendantsOfType<KtClass>()
                .filter { it.isAnnotation() }
                .filter { annotationEntry ->
                    annotationEntry.annotationEntries.any { it.isRequiresOptInWithErrorLevel() }
                }.forEach { ktClass ->
                    ktClass.fqName?.asString()?.let { add(it) }
                }
        }
    }

    private fun KtAnnotationEntry.isRequiresOptInWithErrorLevel(): Boolean {
        return shortName?.asString() == REQUIRES_OPT_IN
    }

    private const val REQUIRES_OPT_IN = "RequiresOptIn"

    // Please do not add new annotations here
    // See KT-62510 for details
    val allowedAnnotations = setOf(
        "kotlin.contracts.ExperimentalContracts",
    )
}