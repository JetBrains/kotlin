/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.Stats.Companion.tcSuite

class PerformanceStressTest : AbstractPerformanceProjectsTest() {

    companion object {

        @JvmStatic
        var warmedUp: Boolean = false

        @JvmStatic
        val hwStats: Stats = Stats("helloWorld project")

        init {
            // there is no @AfterClass for junit3.8
            Runtime.getRuntime().addShutdownHook(Thread { hwStats.close() })
        }

    }

    override fun setUp() {
        super.setUp()
        // warm up: open simple small project
        if (!warmedUp) {
            warmUpProject(hwStats, "src/HelloMain.kt") {
                openProject {
                    name("helloWorld")

                    kotlinFile("HelloMain") {
                        topFunction("main") {
                            param("args", "Array<String>")
                            body("""println("Hello World!")""")
                        }
                    }
                }
            }
            warmedUp = true
        }
    }

    fun testLotsOfOverloadedMethods() {
        // KT-35135
        val generatedTypes = mutableListOf(listOf<String>())
        generateTypes(arrayOf("Int", "String", "Long", "List<Int>", "Array<Int>"), generatedTypes)

        tcSuite("Lots of overloaded method project") {
            myProject = openProject {
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

            val stats = Stats("kt-35135 project")
            stats.use { stats ->
                perfType(
                    stats,
                    "src/main/java/pkg/SomeClass.kt",
                    "ov",
                    "override fun foo(): String = TODO()",
                    note = "override fun foo()",
                    delay = 50
                )
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