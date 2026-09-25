/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.generator

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }
private const val GENERATED_PACKAGE = "org.jetbrains.kotlin.statistics.metrics"
private const val GENERATED_FILE_NAME = "LanguageFeatureValues.kt"

fun main(args: Array<String>) {
    val genDir = File(args[0])
    val outputFile = genDir.resolve(GENERATED_PACKAGE.replace('.', '/')).resolve(GENERATED_FILE_NAME)

    val fileText = buildString {
        with(SmartPrinter(this)) {
            println(COPYRIGHT)
            println("package $GENERATED_PACKAGE")
            println()
            print(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
            println("generator in :kotlin-gradle-statistics:metric-helpers-generator")
            println(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX)
            println()
            println("internal val languageFeatureNames: List<String> = listOf(")
            withIndent {
                for (feature in LanguageFeature.entries) {
                    println("\"${feature.name}\",")
                }
            }
            println(")")
        }
    }

    GeneratorsFileUtil.writeFileIfContentChanged(outputFile, fileText, logNotChanged = false)
}
