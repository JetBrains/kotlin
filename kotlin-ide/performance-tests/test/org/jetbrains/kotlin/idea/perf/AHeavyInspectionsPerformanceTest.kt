/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.perf.util.ExternalProject
import org.jetbrains.kotlin.idea.perf.util.lastPathSegment
import org.jetbrains.kotlin.idea.perf.util.suite
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith

/**
 * Run the only specified exceptions on the selected files in Kotling project.
 * Used for 'The Kotlin sources: kt files heavy inspections' graph.
 * @TODO Should be run before typing tests as the testing project becomes unusable afterwards.
 */
@RunWith(JUnit3RunnerWithInners::class)
class AHeavyInspectionsPerformanceTest : UsefulTestCase() {
    val listOfFiles = arrayOf(
        "libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/NewMultiplatformIT.kt",
        "idea/idea-analysis/src/org/jetbrains/kotlin/idea/util/PsiPrecedences.kt",
        "compiler/psi/src/org/jetbrains/kotlin/psi/KtElement.kt",
        "compiler/psi/src/org/jetbrains/kotlin/psi/KtFile.kt",
        "compiler/psi/src/org/jetbrains/kotlin/psi/KtImportInfo.kt"
    )
    val listOfInspections = arrayOf(
        "UnusedSymbol",
        "MemberVisibilityCanBePrivate"
    )

    fun testLocalInspection() {
        suite {
            app {
                project(ExternalProject.KOTLIN_GRADLE) {
                    for (inspection in listOfInspections) {
                        enableSingleInspection(inspection)
                        for (file in listOfFiles) {
                            val editorFile = editor(file)

                            measure<List<HighlightInfo>>(inspection, file.lastPathSegment()) {
                                test = {
                                    highlight(editorFile)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}