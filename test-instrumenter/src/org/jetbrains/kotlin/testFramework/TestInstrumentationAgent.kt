/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.testFramework

import java.io.File
import java.io.IOException
import java.lang.instrument.Instrumentation
import org.jetbrains.kotlin.testFramework.bootclasspath.FileAccessMonitorAgent
import java.nio.file.Files
import java.nio.file.Paths

@Suppress("unused")
object TestInstrumentationAgent {
    @JvmStatic
    fun premain(arg: String?, instrumentation: Instrumentation) {
        val arguments = arg.orEmpty().split(",")

        val debug = "debug" in arguments
        if (debug) {
            println("org.jetbrains.kotlin.testFramework.TestInstrumentationAgent: premain")
        }

        instrumentation.allLoadedClasses.filter { it.name == "java.nio.file.Files" || it.name == "java.io.File" }.forEach {
            println("ALREADY LOADED ${it.name}")
            println(it.classLoader)
        }

        println(instrumentation.isRetransformClassesSupported)

        instrumentation.addTransformer(MockApplicationCreationTracingInstrumenter(debug))
        instrumentation.addTransformer(FileAccessTransformer(), true)

        // It's loaded? How to prevent double transformation?
        instrumentation.retransformClasses(File::class.java, Files::class.java)

        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                val tempDirPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().toString()
                val javaHomePath = Paths.get(System.getProperty("java.home")).toAbsolutePath().toString()
                File("accessedFiles.txt").writeText(
                    FileAccessMonitorAgent.accessedFiles.filter {
                        !(it.endsWith(".class") || it.endsWith(".jar") || it.startsWith(tempDirPath) || it.startsWith(javaHomePath))
                    }.joinToString("\n")
                )
            } catch (e: IOException) {
                System.err.println("Failed to write accessed files: ${e.message}")
            }
        })
    }
}