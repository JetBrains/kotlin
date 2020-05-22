/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.perf.util.DefaultProfile
import org.jetbrains.kotlin.idea.perf.util.PerformanceSuite
import org.jetbrains.kotlin.idea.perf.util.PerformanceSuite.TypingConfig
import org.jetbrains.kotlin.idea.perf.util.ProjectProfile
import org.jetbrains.kotlin.idea.perf.util.suite
import org.jetbrains.kotlin.idea.testFramework.commitAllDocuments
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith

@RunWith(JUnit3RunnerWithInners::class)
class PerformanceStressTest : UsefulTestCase() {

    fun testLotsOfOverloadedMethods() {
        // KT-35135
        val generatedTypes = mutableListOf(listOf<String>())
        generateTypes(arrayOf("Int", "String", "Long", "List<Int>", "Array<Int>"), generatedTypes)

        suite(
            suiteName = "Lots of overloaded method project",
            config = PerformanceSuite.StatsScopeConfig(name = "kt-35135 project", warmup = 8, iterations = 15)
        ) {
            app {
                warmUpProject()

                project {
                    descriptor {
                        name("kt-35135")
                        buildGradle("idea/testData/perfTest/simpleTemplate/")

                        kotlinFile("OverloadX") {
                            pkg("pkg")

                            topClass("OverloadX") {
                                openClass()

                                for (types in generatedTypes) {
                                    function("foo") {
                                        openFunction()
                                        returnType("String")
                                        for ((index, type) in types.withIndex()) {
                                            param("arg$index", type)
                                        }
                                        body("TODO()")
                                    }
                                }
                            }
                        }

                        kotlinFile("SomeClass") {
                            pkg("pkg")

                            topClass("SomeClass") {
                                superClass("OverloadX")

                                body("ov")
                            }
                        }
                    }

                    profile(DefaultProfile)

                    fixture("src/main/java/pkg/SomeClass.kt").use { fixture ->
                        val typingConfig = TypingConfig(
                            fixture,
                            marker = "ov",
                            insertString = "override fun foo(): String = TODO()",
                            delayMs = 50
                        )

                        measure<List<HighlightInfo>>("type override fun foo()", fixture = fixture) {
                            before = {
                                moveCursor(typingConfig)
                            }
                            test = {
                                typeAndHighlight(typingConfig)
                            }
                            after = {
                                fixture.restoreText()
                                commitAllDocuments()
                            }
                        }
                    }
                }
            }
        }
    }

    private tailrec fun generateTypes(types: Array<String>, results: MutableList<List<String>>, index: Int = 0, maxCount: Int = 3000) {
        val newResults = mutableListOf<List<String>>()
        for (list in results) {
            if (list.size < index) continue
            for (t in types) {
                val newList = mutableListOf<String>()
                newList.addAll(list)
                newList.add(t)
                newResults.add(newList.toList())
                if (results.size + newResults.size >= maxCount) {
                    results.addAll(newResults)
                    return
                }
            }
        }
        results.addAll(newResults)
        generateTypes(types, results, index + 1, maxCount)
    }
}
