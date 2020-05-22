/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

/**
 * warm up: open simple `hello world` project
 */
class WarmUpProject(private val stats: Stats) {
    private var warmedUp: Boolean = false

    fun warmUp(test: AbstractPerformanceProjectsTest) {
        if (warmedUp) return
        test.warmUpProject(stats, "src/HelloMain.kt") {
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